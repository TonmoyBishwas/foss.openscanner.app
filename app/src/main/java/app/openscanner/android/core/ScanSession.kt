package app.openscanner.android.core

import android.graphics.Bitmap
import app.openscanner.android.core.vision.Quad

/**
 * In-memory hand-off between scanner -> crop -> review screens.
 * Bitmaps are far too large for navigation arguments, so the screens share
 * this single-scan scratch state instead.
 */
object ScanSession {
    /** Full-resolution upright capture. */
    var captured: Bitmap? = null

    /** Auto-detected quad in [captured] pixel coordinates, if any. */
    var detectedQuad: Quad? = null

    /** Result of the perspective crop, input to the filter step. */
    var cropped: Bitmap? = null

    fun clear() {
        captured = null
        detectedQuad = null
        cropped = null
    }
}
