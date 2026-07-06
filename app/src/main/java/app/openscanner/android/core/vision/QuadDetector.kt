package app.openscanner.android.core.vision

import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Size
import org.opencv.geometry.Geometry
import org.opencv.imgproc.Imgproc

/**
 * Finds the largest document-like quadrilateral in a grayscale frame.
 *
 * Pipeline: Gaussian blur -> Canny (Otsu-derived thresholds) -> morphological
 * close -> contours -> approxPolyDP -> first convex 4-gon covering enough of
 * the frame.
 */
class QuadDetector(
    /** Minimum quad area as a fraction of the frame area. */
    private val minAreaFraction: Float = 0.10f
) {

    /**
     * @param gray upright single-channel frame (already rotated to display orientation)
     * @return the detected quad in pixel coordinates of [gray], or null
     */
    fun detect(gray: Mat): Quad? {
        val frameArea = (gray.width() * gray.height()).toFloat()

        val blurred = Mat()
        Imgproc.GaussianBlur(gray, blurred, Size(5.0, 5.0), 0.0)

        // Otsu picks a global threshold; Canny at [t/2, t] tracks scene contrast.
        val otsuDst = Mat()
        val otsu = Imgproc.threshold(blurred, otsuDst, 0.0, 255.0, Imgproc.THRESH_BINARY or Imgproc.THRESH_OTSU)
        otsuDst.release()
        val edges = Mat()
        Imgproc.Canny(blurred, edges, otsu * 0.5, otsu)

        val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(5.0, 5.0))
        Imgproc.morphologyEx(edges, edges, Imgproc.MORPH_CLOSE, kernel)

        val contours = ArrayList<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(edges, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)
        hierarchy.release()

        val best = contours
            .sortedByDescending { Geometry.contourArea(it) }
            .take(5)
            .firstNotNullOfOrNull { contour -> approxQuad(contour, frameArea) }

        blurred.release()
        edges.release()
        kernel.release()
        contours.forEach { it.release() }

        return best
    }

    private fun approxQuad(contour: MatOfPoint, frameArea: Float): Quad? {
        val points2f = MatOfPoint2f(*contour.toArray())
        val peri = Geometry.arcLength(points2f, true)
        val approx = MatOfPoint2f()
        Geometry.approxPolyDP(points2f, approx, 0.02 * peri, true)

        val quad = if (approx.total() == 4L) {
            val approxPoints = MatOfPoint(*approx.toArray().map { org.opencv.core.Point(it.x, it.y) }.toTypedArray())
            val convex = Geometry.isContourConvex(approxPoints)
            val area = Geometry.contourArea(approx).toFloat()
            approxPoints.release()
            if (convex && area >= frameArea * minAreaFraction) {
                Quad.fromUnordered(approx.toArray().map { Point2(it.x.toFloat(), it.y.toFloat()) })
            } else null
        } else null

        points2f.release()
        approx.release()
        return quad
    }
}
