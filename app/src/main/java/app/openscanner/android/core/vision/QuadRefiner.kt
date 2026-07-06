package app.openscanner.android.core.vision

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

/**
 * Snaps a coarse quad precisely onto the page border: samples gradient
 * profiles perpendicular to each edge, moves every sample to the strongest
 * gradient ridge nearby, refits each edge as a least-squares line and
 * intersects adjacent lines into sub-pixel corners.
 *
 * This corrects the two systematic errors of contour-based detection: the
 * outward offset introduced by edge dilation, and attachment to a page's
 * drop shadow instead of the page itself (the page border is always the
 * strongest gradient in the neighborhood).
 */
object QuadRefiner {

    fun refine(gray: Mat, quad: Quad, searchRadius: Float = 12f, samplesPerEdge: Int = 28): Quad {
        val width = gray.width()
        val height = gray.height()

        val blurred = Mat()
        Imgproc.GaussianBlur(gray, blurred, Size(3.0, 3.0), 0.0)
        val gx = Mat()
        val gy = Mat()
        Imgproc.Sobel(blurred, gx, CvType.CV_32F, 1, 0, 3)
        Imgproc.Sobel(blurred, gy, CvType.CV_32F, 0, 1, 3)
        blurred.release()
        val magnitude = Mat()
        Core.magnitude(gx, gy, magnitude)
        gx.release()
        gy.release()
        val mag = FloatArray(width * height)
        magnitude.get(0, 0, mag)
        magnitude.release()

        val corners = quad.corners
        val lines = ArrayList<Pair<Point2, Point2>>(4)
        for (i in 0 until 4) {
            val a = corners[i]
            val b = corners[(i + 1) % 4]
            lines.add(refineEdge(mag, width, height, a, b, searchRadius, samplesPerEdge))
        }

        val refined = ArrayList<Point2>(4)
        val maxShift = searchRadius * 2.5f
        for (i in 0 until 4) {
            val corner = intersect(lines[(i + 3) % 4], lines[i])
            refined.add(
                if (corner != null && corner.distanceTo(corners[i]) <= maxShift) corner
                else corners[i]
            )
        }
        return Quad.fromUnordered(refined)
    }

    /** Snap one edge to the gradient ridge; returns two points defining the fitted line. */
    private fun refineEdge(
        mag: FloatArray,
        width: Int,
        height: Int,
        a: Point2,
        b: Point2,
        searchRadius: Float,
        samples: Int
    ): Pair<Point2, Point2> {
        val edgeX = b.x - a.x
        val edgeY = b.y - a.y
        val len = hypot(edgeX, edgeY)
        if (len < 1f) return a to b
        val nx = -edgeY / len
        val ny = edgeX / len

        val xs = ArrayList<Float>(samples)
        val ys = ArrayList<Float>(samples)
        for (s in 0 until samples) {
            val t = 0.12f + (0.76f * s) / (samples - 1)
            val px = a.x * (1 - t) + b.x * t
            val py = a.y * (1 - t) + b.y * t
            var bestOffset = 0f
            var bestValue = -1f
            var offset = -searchRadius
            while (offset <= searchRadius) {
                val x = (px + nx * offset).roundToInt()
                val y = (py + ny * offset).roundToInt()
                if (x in 0 until width && y in 0 until height) {
                    val v = mag[y * width + x]
                    if (v > bestValue) {
                        bestValue = v
                        bestOffset = offset
                    }
                }
                offset += 0.5f
            }
            if (bestValue > 15f) {
                xs.add(px + nx * bestOffset)
                ys.add(py + ny * bestOffset)
            }
        }
        if (xs.size < samples / 2) return a to b // weak edge: keep the original

        // Least-squares line via PCA: centroid + principal direction.
        val mx = xs.average().toFloat()
        val my = ys.average().toFloat()
        var sxx = 0f
        var sxy = 0f
        var syy = 0f
        for (i in xs.indices) {
            val dx = xs[i] - mx
            val dy = ys[i] - my
            sxx += dx * dx
            sxy += dx * dy
            syy += dy * dy
        }
        val theta = 0.5f * atan2(2f * sxy, sxx - syy)
        val dx = cos(theta)
        val dy = sin(theta)
        return Point2(mx, my) to Point2(mx + dx, my + dy)
    }

    private fun intersect(l1: Pair<Point2, Point2>, l2: Pair<Point2, Point2>): Point2? {
        val d1x = l1.second.x - l1.first.x
        val d1y = l1.second.y - l1.first.y
        val d2x = l2.second.x - l2.first.x
        val d2y = l2.second.y - l2.first.y
        val denom = d1x * d2y - d1y * d2x
        if (abs(denom) < 1e-6f) return null
        val t = ((l2.first.x - l1.first.x) * d2y - (l2.first.y - l1.first.y) * d2x) / denom
        return Point2(l1.first.x + d1x * t, l1.first.y + d1y * t)
    }
}
