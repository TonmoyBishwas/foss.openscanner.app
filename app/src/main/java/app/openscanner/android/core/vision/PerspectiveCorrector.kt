package app.openscanner.android.core.vision

import android.graphics.Bitmap
import kotlin.math.roundToInt
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.geometry.Geometry
import org.opencv.imgproc.Imgproc

/**
 * Warps the region inside a quad to a flat, upright rectangle — the "scanner"
 * de-skew. Output size follows the longest opposing edges so the page keeps
 * its physical proportions.
 */
object PerspectiveCorrector {

    fun crop(source: Bitmap, quad: Quad): Bitmap {
        val width = maxOf(
            quad.topLeft.distanceTo(quad.topRight),
            quad.bottomLeft.distanceTo(quad.bottomRight)
        ).roundToInt().coerceAtLeast(1)
        val height = maxOf(
            quad.topLeft.distanceTo(quad.bottomLeft),
            quad.topRight.distanceTo(quad.bottomRight)
        ).roundToInt().coerceAtLeast(1)

        val srcMat = Mat()
        Utils.bitmapToMat(source, srcMat)

        val srcPoints = MatOfPoint2f(
            quad.topLeft.toCv(),
            quad.topRight.toCv(),
            quad.bottomRight.toCv(),
            quad.bottomLeft.toCv()
        )
        val dstPoints = MatOfPoint2f(
            Point(0.0, 0.0),
            Point(width - 1.0, 0.0),
            Point(width - 1.0, height - 1.0),
            Point(0.0, height - 1.0)
        )

        val transform = Geometry.getPerspectiveTransform(srcPoints, dstPoints)
        val warped = Mat()
        Imgproc.warpPerspective(srcMat, warped, transform, Size(width.toDouble(), height.toDouble()))

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(warped, result)

        srcMat.release()
        srcPoints.release()
        dstPoints.release()
        transform.release()
        warped.release()
        return result
    }

    private fun Point2.toCv() = Point(x.toDouble(), y.toDouble())
}
