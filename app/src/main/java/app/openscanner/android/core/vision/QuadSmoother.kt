package app.openscanner.android.core.vision

/**
 * Temporal smoothing for the live overlay: exponential moving average over
 * detected corners so the quad glides instead of jittering, with a snap when
 * the detection jumps to a genuinely different place, and a grace period of
 * missed frames before the overlay disappears.
 */
class QuadSmoother(
    /** Smoothing for barely-moving quads: heavy, so the overlay sits still. */
    private val alphaCalm: Float = 0.18f,
    /** Smoothing for fast motion: light, so the overlay keeps up. */
    private val alphaMoving: Float = 0.55f,
    /** Movement (fraction of frame diagonal) treated as "fast". */
    private val movingDistanceFraction: Float = 0.06f,
    /** Corner jump (fraction of frame diagonal) beyond which we snap instead of glide. */
    private val snapDistanceFraction: Float = 0.15f,
    /** Consecutive misses tolerated before the quad is dropped. */
    private val maxMisses: Int = 10
) {
    private var current: Quad? = null
    private var misses = 0

    /** Last smoothed quad, if any — feed back into detection for stickiness. */
    fun current(): Quad? = current

    fun update(detected: Quad?, frameDiagonal: Float): Quad? {
        val previous = current
        if (detected == null) {
            misses++
            if (misses > maxMisses) current = null
            return current
        }
        misses = 0
        val distance = previous?.distanceTo(detected) ?: Float.MAX_VALUE
        current = if (previous == null || distance > frameDiagonal * snapDistanceFraction) {
            detected
        } else {
            // Adaptive smoothing: still overlay while holding steady, fast
            // tracking while the camera moves.
            val speed = (distance / (frameDiagonal * movingDistanceFraction)).coerceIn(0f, 1f)
            val alpha = alphaCalm + (alphaMoving - alphaCalm) * speed
            Quad(
                lerp(previous.topLeft, detected.topLeft, alpha),
                lerp(previous.topRight, detected.topRight, alpha),
                lerp(previous.bottomRight, detected.bottomRight, alpha),
                lerp(previous.bottomLeft, detected.bottomLeft, alpha)
            )
        }
        return current
    }

    fun reset() {
        current = null
        misses = 0
    }

    private fun lerp(from: Point2, to: Point2, alpha: Float): Point2 =
        Point2(from.x + (to.x - from.x) * alpha, from.y + (to.y - from.y) * alpha)
}
