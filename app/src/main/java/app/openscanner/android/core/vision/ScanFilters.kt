package app.openscanner.android.core.vision

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.CLAHE
import org.opencv.imgproc.Imgproc

enum class ScanFilter(val label: String) {
    MagicBw("Magic B&W"),
    MagicColor("Color"),
    Grayscale("Grayscale"),
    Original("Original")
}

/**
 * Post-capture enhancement filters. The signature move is Magic B&W:
 * illumination flattening (divide the page by its blurred background, which
 * erases shadows and uneven light) followed by adaptive binarization, giving
 * crisp black text on clean white.
 */
object ScanFilters {

    fun apply(source: Bitmap, filter: ScanFilter): Bitmap {
        if (filter == ScanFilter.Original) return source

        val rgba = Mat()
        Utils.bitmapToMat(source, rgba)

        val out: Mat = when (filter) {
            ScanFilter.MagicBw -> magicBw(rgba)
            ScanFilter.MagicColor -> magicColor(rgba)
            ScanFilter.Grayscale -> grayscale(rgba)
            ScanFilter.Original -> rgba
        }

        val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(out, result)
        rgba.release()
        if (out !== rgba) out.release()
        return result
    }

    /** Divide the grayscale page by its morphologically-estimated background. */
    private fun flattenIllumination(gray: Mat): Mat {
        val background = Mat()
        val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(7.0, 7.0))
        Imgproc.dilate(gray, background, kernel)
        Imgproc.medianBlur(background, background, 21)
        val flat = Mat()
        Core.divide(gray, background, flat, 255.0)
        background.release()
        kernel.release()
        return flat
    }

    private fun magicBw(rgba: Mat): Mat {
        val gray = Mat()
        Imgproc.cvtColor(rgba, gray, Imgproc.COLOR_RGBA2GRAY)
        val flat = flattenIllumination(gray)
        gray.release()

        // Block size scales with resolution so large captures binarize evenly.
        val block = (minOf(flat.width(), flat.height()) / 40).coerceAtLeast(15) or 1
        val binary = Mat()
        Imgproc.adaptiveThreshold(
            flat, binary, 255.0,
            Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY,
            block, 12.0
        )
        flat.release()

        // Gentle despeckle that keeps thin strokes.
        Imgproc.medianBlur(binary, binary, 3)
        return binary
    }

    private fun magicColor(rgba: Mat): Mat {
        val rgb = Mat()
        Imgproc.cvtColor(rgba, rgb, Imgproc.COLOR_RGBA2RGB)
        val lab = Mat()
        Imgproc.cvtColor(rgb, lab, Imgproc.COLOR_RGB2Lab)
        rgb.release()

        val channels = ArrayList<Mat>(3)
        Core.split(lab, channels)
        val flatL = flattenIllumination(channels[0])
        channels[0].release()

        val clahe: CLAHE = Imgproc.createCLAHE(2.0, Size(8.0, 8.0))
        clahe.apply(flatL, flatL)
        channels[0] = flatL

        Core.merge(channels, lab)
        channels.forEach { it.release() }

        val out = Mat()
        Imgproc.cvtColor(lab, out, Imgproc.COLOR_Lab2RGB)
        lab.release()
        return out
    }

    private fun grayscale(rgba: Mat): Mat {
        val gray = Mat()
        Imgproc.cvtColor(rgba, gray, Imgproc.COLOR_RGBA2GRAY)
        val flat = flattenIllumination(gray)
        gray.release()
        val clahe: CLAHE = Imgproc.createCLAHE(2.0, Size(8.0, 8.0))
        clahe.apply(flat, flat)
        return flat
    }
}
