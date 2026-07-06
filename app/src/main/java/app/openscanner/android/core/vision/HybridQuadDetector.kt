package app.openscanner.android.core.vision

import kotlin.math.hypot
import org.opencv.core.Mat

/**
 * Combines the neural and classical detectors:
 *
 * - The ML model (trained on real photos) is far more robust at *finding*
 *   the page in messy scenes, but its corners are coarse (256px input).
 * - The classical detector localizes precisely but can lose the page.
 *
 * So: when both agree it's the same page, take the classical corners; when
 * only the ML model is confident, refine its coarse corners against the
 * image gradient; when the model is unavailable or unsure, classical alone.
 */
class HybridQuadDetector(
    private val ml: MlQuadDetector?,
    private val classical: QuadDetector = QuadDetector()
) {

    /**
     * @param rgba upright RGBA frame
     * @param gray the same frame in grayscale
     */
    fun detect(rgba: Mat, gray: Mat, previous: Quad? = null): Quad? {
        val diagonal = hypot(gray.width().toFloat(), gray.height().toFloat())
        val mlQuad = ml?.detect(rgba)
        val classicalQuad = classical.detect(gray, previous) // already refined
        return when {
            mlQuad != null && classicalQuad != null ->
                if (mlQuad.distanceTo(classicalQuad) < diagonal * 0.05f) classicalQuad
                else QuadRefiner.refine(gray, mlQuad)
            mlQuad != null -> QuadRefiner.refine(gray, mlQuad)
            else -> classicalQuad
        }
    }
}
