package app.openscanner.android.core.vision

import kotlin.math.max
import kotlin.math.min

/**
 * Maps points from an upright camera frame to the viewfinder box, assuming the
 * viewfinder scales the frame with fill-center (crop-to-fill) like
 * CameraXViewfinder does.
 */
object CoordinateMapper {

    fun mapPoint(
        p: Point2,
        frameWidth: Float,
        frameHeight: Float,
        viewWidth: Float,
        viewHeight: Float
    ): Point2 {
        val scale = max(viewWidth / frameWidth, viewHeight / frameHeight)
        val offsetX = (viewWidth - frameWidth * scale) / 2f
        val offsetY = (viewHeight - frameHeight * scale) / 2f
        return Point2(p.x * scale + offsetX, p.y * scale + offsetY)
    }

    fun mapQuad(
        quad: Quad,
        frameWidth: Float,
        frameHeight: Float,
        viewWidth: Float,
        viewHeight: Float
    ): Quad = quad.map { mapPoint(it, frameWidth, frameHeight, viewWidth, viewHeight) }

    // Fit-center (letterbox) mapping, used when a still image is shown whole,
    // e.g. in the crop editor.

    fun fitScale(frameWidth: Float, frameHeight: Float, viewWidth: Float, viewHeight: Float): Float =
        min(viewWidth / frameWidth, viewHeight / frameHeight)

    fun mapPointFit(
        p: Point2,
        frameWidth: Float,
        frameHeight: Float,
        viewWidth: Float,
        viewHeight: Float
    ): Point2 {
        val scale = fitScale(frameWidth, frameHeight, viewWidth, viewHeight)
        val offsetX = (viewWidth - frameWidth * scale) / 2f
        val offsetY = (viewHeight - frameHeight * scale) / 2f
        return Point2(p.x * scale + offsetX, p.y * scale + offsetY)
    }

    fun mapQuadFit(
        quad: Quad,
        frameWidth: Float,
        frameHeight: Float,
        viewWidth: Float,
        viewHeight: Float
    ): Quad = quad.map { mapPointFit(it, frameWidth, frameHeight, viewWidth, viewHeight) }

    /** Inverse of [mapPointFit]: view coordinates back to image pixels (unclamped). */
    fun unmapPointFit(
        p: Point2,
        frameWidth: Float,
        frameHeight: Float,
        viewWidth: Float,
        viewHeight: Float
    ): Point2 {
        val scale = fitScale(frameWidth, frameHeight, viewWidth, viewHeight)
        val offsetX = (viewWidth - frameWidth * scale) / 2f
        val offsetY = (viewHeight - frameHeight * scale) / 2f
        return Point2((p.x - offsetX) / scale, (p.y - offsetY) / scale)
    }
}
