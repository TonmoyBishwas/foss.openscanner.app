package app.openscanner.android.core.vision

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QuadSmootherTest {

    private val diagonal = 800f

    private fun quadAt(x: Float, y: Float) = Quad(
        Point2(x, y), Point2(x + 100f, y),
        Point2(x + 100f, y + 100f), Point2(x, y + 100f)
    )

    @Test
    fun `first detection snaps immediately`() {
        val smoother = QuadSmoother()
        val result = smoother.update(quadAt(50f, 50f), diagonal)
        assertEquals(quadAt(50f, 50f), result)
    }

    @Test
    fun `small movement is smoothed not snapped`() {
        val smoother = QuadSmoother(alphaCalm = 0.5f, alphaMoving = 0.5f)
        smoother.update(quadAt(0f, 0f), diagonal)
        val result = smoother.update(quadAt(10f, 0f), diagonal)
        assertNotNull(result)
        // halfway between 0 and 10 with alpha pinned to 0.5
        assertEquals(5f, result!!.topLeft.x, 0.01f)
    }

    @Test
    fun `tiny movement gets heavier smoothing than fast movement`() {
        val calm = QuadSmoother()
        calm.update(quadAt(0f, 0f), diagonal)
        val calmResult = calm.update(quadAt(2f, 0f), diagonal)!!

        val moving = QuadSmoother()
        moving.update(quadAt(0f, 0f), diagonal)
        val movingResult = moving.update(quadAt(60f, 0f), diagonal)!!

        val calmFraction = calmResult.topLeft.x / 2f
        val movingFraction = movingResult.topLeft.x / 60f
        assertTrue(
            "calm=$calmFraction should lag more than moving=$movingFraction",
            calmFraction < movingFraction
        )
    }

    @Test
    fun `large jump snaps to new quad`() {
        val smoother = QuadSmoother()
        smoother.update(quadAt(0f, 0f), diagonal)
        val far = quadAt(500f, 300f)
        val result = smoother.update(far, diagonal)
        assertEquals(far, result)
    }

    @Test
    fun `misses within grace keep last quad`() {
        val smoother = QuadSmoother(maxMisses = 3)
        smoother.update(quadAt(0f, 0f), diagonal)
        repeat(3) { assertNotNull(smoother.update(null, diagonal)) }
    }

    @Test
    fun `quad drops after too many misses`() {
        val smoother = QuadSmoother(maxMisses = 2)
        smoother.update(quadAt(0f, 0f), diagonal)
        smoother.update(null, diagonal)
        smoother.update(null, diagonal)
        assertNull(smoother.update(null, diagonal))
    }

    @Test
    fun `reset clears state`() {
        val smoother = QuadSmoother()
        smoother.update(quadAt(0f, 0f), diagonal)
        smoother.reset()
        assertNull(smoother.update(null, diagonal))
    }
}
