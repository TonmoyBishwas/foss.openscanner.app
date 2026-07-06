package app.openscanner.android.core.vision

import androidx.camera.core.ImageProxy
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat

/**
 * Extracts the Y (luminance) plane of a YUV_420_888 frame as a grayscale Mat.
 * Detection only needs luminance, so we skip full YUV->RGB conversion.
 */
fun ImageProxy.toGrayMat(): Mat {
    val plane = planes[0]
    val buffer = plane.buffer.duplicate()
    val mat = Mat(height, width, CvType.CV_8UC1)
    if (plane.rowStride == width) {
        val bytes = ByteArray(minOf(buffer.remaining(), width * height))
        buffer.get(bytes)
        mat.put(0, 0, bytes)
    } else {
        val row = ByteArray(width)
        for (y in 0 until height) {
            buffer.position(y * plane.rowStride)
            buffer.get(row, 0, minOf(width, buffer.remaining()))
            mat.put(y, 0, row)
        }
    }
    return mat
}

/** Returns an upright copy rotated by [degrees] clockwise (releases nothing). */
fun Mat.rotated(degrees: Int): Mat = when ((degrees % 360 + 360) % 360) {
    90 -> Mat().also { Core.rotate(this, it, Core.ROTATE_90_CLOCKWISE) }
    180 -> Mat().also { Core.rotate(this, it, Core.ROTATE_180) }
    270 -> Mat().also { Core.rotate(this, it, Core.ROTATE_90_COUNTERCLOCKWISE) }
    else -> this
}
