package app.openscanner.android.core.vision

import kotlin.math.acos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfInt
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.geometry.Geometry
import org.opencv.imgproc.Imgproc

/**
 * Finds the document quadrilateral in a grayscale frame.
 *
 * Real photos rarely yield one clean page contour, so this runs FOUR candidate
 * generators (low-threshold Canny, CLAHE-boosted Canny for weak borders,
 * adaptive threshold, global Otsu), turns large contours into quads via
 * convex hull + multi-epsilon approxPolyDP, then validates each candidate
 * against the image gradient: a real page has edge response along its whole
 * perimeter, illumination ghosts don't. The best-scoring validated quad wins;
 * if none survives, we return null and the UI falls back to manual cropping —
 * a missing overlay beats a wrong one.
 *
 * Tuned against the synthetic photo suite in androidTest assets (clutter,
 * shadows, hands, low contrast, newspaper backgrounds) — see
 * QuadDetectorRealismTest.
 */
class QuadDetector(
    /** Minimum quad area as a fraction of the frame area. */
    private val minAreaFraction: Float = 0.08f
) {

    private class Candidate(val quad: Quad, val areaFraction: Float, val rectangularity: Float)

    /**
     * @param previous last known quad (smoothed), if any — candidates close to
     *   it get a stickiness bonus so the winner doesn't swap between similar
     *   candidates frame over frame (visible as overlay jumping)
     */
    fun detect(gray: Mat, previous: Quad? = null): Quad? {
        val width = gray.width()
        val height = gray.height()
        val frameArea = (width * height).toFloat()
        val diagonal = kotlin.math.hypot(width.toFloat(), height.toFloat())

        // Median blur kills sensor noise that would otherwise inflate both the
        // edge maps and the gradient validation below.
        val blurred = Mat()
        Imgproc.medianBlur(gray, blurred, 3)
        Imgproc.GaussianBlur(blurred, blurred, Size(5.0, 5.0), 0.0)

        val gradient = GradientMap(blurred)
        val candidates = ArrayList<Candidate>()
        val kernel3 = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(3.0, 3.0))

        // Path A: low-threshold Canny + dilation to bridge border gaps.
        // Thresholds stay low so soft page borders survive; false edges are
        // rejected later by gradient validation, not here.
        val edges = Mat()
        Imgproc.Canny(blurred, edges, 30.0, 90.0)
        Imgproc.dilate(edges, edges, kernel3, Point(-1.0, -1.0), 2)
        collectQuads(edges, frameArea, candidates)
        edges.release()

        // Path B: CLAHE-boosted Canny rescues weak borders in low contrast.
        val clahe = Mat()
        Imgproc.createCLAHE(3.0, Size(8.0, 8.0)).apply(blurred, clahe)
        val edges2 = Mat()
        Imgproc.Canny(clahe, edges2, 30.0, 90.0)
        Imgproc.dilate(edges2, edges2, kernel3, Point(-1.0, -1.0), 2)
        collectQuads(edges2, frameArea, candidates)
        clahe.release()
        edges2.release()

        // Path C: adaptive threshold — the page reads as one bright blob even
        // under uneven lighting.
        val adaptive = Mat()
        Imgproc.adaptiveThreshold(
            blurred, adaptive, 255.0,
            Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 51, -4.0
        )
        val kernel5 = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(5.0, 5.0))
        Imgproc.morphologyEx(adaptive, adaptive, Imgproc.MORPH_OPEN, kernel5)
        collectQuads(adaptive, frameArea, candidates)
        adaptive.release()
        kernel5.release()

        // Path D: global Otsu — cheap win in well-lit high-contrast scenes.
        val otsu = Mat()
        Imgproc.threshold(blurred, otsu, 0.0, 255.0, Imgproc.THRESH_BINARY or Imgproc.THRESH_OTSU)
        collectQuads(otsu, frameArea, candidates)
        otsu.release()

        kernel3.release()
        blurred.release()

        var best: Quad? = null
        var bestScore = 0f
        for (candidate in candidates) {
            val support = gradient.edgeSupport(candidate.quad)
            if (support.mean < 0.6f || support.weakestEdge < 0.35f) continue
            val borderPenalty = 1f - 0.5f * borderFraction(candidate.quad, width, height)
            val stickiness = if (previous != null) {
                0.15f * kotlin.math.exp(-(candidate.quad.distanceTo(previous) / diagonal) * 12f)
            } else 0f
            val score = (0.45f * support.mean +
                0.30f * candidate.rectangularity +
                0.25f * min(candidate.areaFraction, 0.9f) +
                stickiness) * borderPenalty
            if (score > bestScore) {
                best = candidate.quad
                bestScore = score
            }
        }
        gradient.release()
        // Sub-pixel snap onto the actual page border (also detaches from
        // drop shadows the coarse contour may have included).
        return best?.let { QuadRefiner.refine(gray, it) }
    }

    private fun collectQuads(binary: Mat, frameArea: Float, out: MutableList<Candidate>) {
        val contours = ArrayList<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(binary, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)
        hierarchy.release()

        val byArea = contours.sortedByDescending { Geometry.contourArea(it) }
        for ((index, contour) in byArea.withIndex()) {
            if (index >= 10 || Geometry.contourArea(contour) < frameArea * minAreaFraction) break

            val hullIndices = MatOfInt()
            Geometry.convexHull(contour, hullIndices)
            val contourPoints = contour.toArray()
            val hullPoints = hullIndices.toArray().map { contourPoints[it] }.toTypedArray()
            hullIndices.release()

            val hull2f = MatOfPoint2f(*hullPoints)
            val peri = Geometry.arcLength(hull2f, true)
            for (epsilon in EPSILONS) {
                val approx = MatOfPoint2f()
                Geometry.approxPolyDP(hull2f, approx, epsilon * peri, true)
                val count = approx.total().toInt()
                if (count == 4) {
                    evaluateQuad(approx, frameArea)?.let(out::add)
                }
                approx.release()
                if (count <= 4) break
            }
            hull2f.release()
        }
        contours.forEach { it.release() }
    }

    private fun evaluateQuad(approx: MatOfPoint2f, frameArea: Float): Candidate? {
        val area = Geometry.contourArea(approx).toFloat()
        if (area < frameArea * minAreaFraction) return null

        val quad = Quad.fromUnordered(approx.toArray().map { Point2(it.x.toFloat(), it.y.toFloat()) })
        if (!anglesSane(quad)) return null

        val rect = Geometry.minAreaRect(approx)
        val rectArea = (rect.size.width * rect.size.height).toFloat()
        if (rectArea <= 0f) return null
        return Candidate(quad, area / frameArea, area / rectArea)
    }

    /** All interior angles must look like page corners seen in perspective. */
    private fun anglesSane(quad: Quad, lo: Float = 45f, hi: Float = 135f): Boolean {
        val c = quad.corners
        for (i in 0 until 4) {
            val p = c[i]
            val a = c[(i + 3) % 4]
            val b = c[(i + 1) % 4]
            val v1x = a.x - p.x; val v1y = a.y - p.y
            val v2x = b.x - p.x; val v2y = b.y - p.y
            val norm = hypot(v1x, v1y) * hypot(v2x, v2y)
            if (norm <= 0f) return false
            val cos = ((v1x * v2x + v1y * v2y) / norm).coerceIn(-1f, 1f)
            val angle = Math.toDegrees(acos(cos.toDouble())).toFloat()
            if (angle < lo || angle > hi) return false
        }
        return true
    }

    /** Fraction of corners sitting on the frame border. */
    private fun borderFraction(quad: Quad, width: Int, height: Int, margin: Float = 3f): Float {
        var on = 0
        for (p in quad.corners) {
            if (p.x <= margin || p.y <= margin ||
                p.x >= width - 1 - margin || p.y >= height - 1 - margin
            ) on++
        }
        return on / 4f
    }

    /**
     * Sobel gradient magnitude of the frame, bulk-copied once so per-quad
     * validation is plain array reads instead of JNI calls.
     */
    private class GradientMap(blurred: Mat) {
        val width = blurred.width()
        val height = blurred.height()
        private val data: FloatArray
        private val threshold: Float

        init {
            val gx = Mat()
            val gy = Mat()
            Imgproc.Sobel(blurred, gx, CvType.CV_32F, 1, 0, 3)
            Imgproc.Sobel(blurred, gy, CvType.CV_32F, 0, 1, 3)
            val magnitude = Mat()
            Core.magnitude(gx, gy, magnitude)
            gx.release()
            gy.release()
            data = FloatArray(width * height)
            magnitude.get(0, 0, data)
            magnitude.release()

            // Threshold adapts to the frame's noise floor (median of a
            // subsample; the full array would be overkill).
            val sample = FloatArray((data.size + 15) / 16) { data[it * 16] }
            sample.sort()
            threshold = max(12f, 2.5f * sample[sample.size / 2])
        }

        class Support(val mean: Float, val weakestEdge: Float)

        /** Per-edge fraction of perimeter samples backed by real gradient. */
        fun edgeSupport(quad: Quad, samplesPerEdge: Int = 24): Support {
            val corners = quad.corners
            var sum = 0f
            var weakest = 1f
            for (i in 0 until 4) {
                val a = corners[i]
                val b = corners[(i + 1) % 4]
                var supported = 0
                for (s in 0 until samplesPerEdge) {
                    val t = 0.08f + (0.84f * s) / (samplesPerEdge - 1)
                    val x = (a.x * (1 - t) + b.x * t).roundToInt()
                    val y = (a.y * (1 - t) + b.y * t).roundToInt()
                    if (x in 1 until width - 1 && y in 1 until height - 1 && windowHit(x, y)) {
                        supported++
                    }
                }
                val fraction = supported.toFloat() / samplesPerEdge
                sum += fraction
                weakest = min(weakest, fraction)
            }
            return Support(sum / 4f, weakest)
        }

        /** 3x3 window max above threshold — tolerates 1px quad inaccuracy. */
        private fun windowHit(x: Int, y: Int): Boolean {
            for (dy in -1..1) {
                val row = (y + dy) * width
                for (dx in -1..1) {
                    if (data[row + x + dx] > threshold) return true
                }
            }
            return false
        }

        fun release() = Unit // data is JVM-managed; Mats already released
    }

    private companion object {
        val EPSILONS = doubleArrayOf(0.02, 0.032, 0.05, 0.08)
    }
}
