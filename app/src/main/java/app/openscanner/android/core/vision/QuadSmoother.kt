package app.openscanner.android.core.vision

/**
 * Temporal smoothing for the live overlay: exponential moving average over
 * detected corners so the quad glides instead of jittering, with a snap when
 * the detection jumps to a genuinely different place, and a grace period of
 * missed frames before the overlay disappears.
 */
class QuadSmoother(
    private val alpha: Float = 0.35f,
    /** Corner jump (fraction of frame diagonal) beyond which we snap instead of glide. */
    private val snapDistanceFraction: Float = 0.15f,
    /** Consecutive misses tolerated before the quad is dropped. */
    private val maxMisses: Int = 10
) {
    private var current: Quad? = null
    private var misses = 0

    fun update(detected: Quad?, frameDiagonal: Float): Quad? {
        val previous = current
        if (detected == null) {
            misses++
            if (misses > maxMisses) current = null
            return current
        }
        misses = 0
        current = if (previous == null || previous.distanceTo(detected) > frameDiagonal * snapDistanceFraction) {
            detected
        } else {
            Quad(
                lerp(previous.topLeft, detected.topLeft),
                lerp(previous.topRight, detected.topRight),
                lerp(previous.bottomRight, detected.bottomRight),
                lerp(previous.bottomLeft, detected.bottomLeft)
            )
        }
        return current
    }

    fun reset() {
        current = null
        misses = 0
    }

    private fun lerp(from: Point2, to: Point2): Point2 =
        Point2(from.x + (to.x - from.x) * alpha, from.y + (to.y - from.y) * alpha)
}
