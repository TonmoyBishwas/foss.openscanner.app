package app.openscanner.android.core.vision

import org.junit.Assert.assertEquals
import org.junit.Test

class QuadTest {

    @Test
    fun `fromUnordered orders corners TL TR BR BL`() {
        val tl = Point2(10f, 10f)
        val tr = Point2(100f, 12f)
        val br = Point2(105f, 90f)
        val bl = Point2(8f, 95f)
        val quad = Quad.fromUnordered(listOf(br, tl, bl, tr))
        assertEquals(tl, quad.topLeft)
        assertEquals(tr, quad.topRight)
        assertEquals(br, quad.bottomRight)
        assertEquals(bl, quad.bottomLeft)
    }

    @Test
    fun `area of axis-aligned rectangle`() {
        val quad = Quad(
            Point2(0f, 0f), Point2(100f, 0f),
            Point2(100f, 50f), Point2(0f, 50f)
        )
        assertEquals(5000f, quad.area(), 0.01f)
    }

    @Test
    fun `scaled multiplies coordinates`() {
        val quad = Quad(
            Point2(1f, 2f), Point2(3f, 2f),
            Point2(3f, 4f), Point2(1f, 4f)
        )
        val scaled = quad.scaled(2f, 10f)
        assertEquals(Point2(2f, 20f), scaled.topLeft)
        assertEquals(Point2(6f, 40f), scaled.bottomRight)
    }

    @Test
    fun `distanceTo averages corner distances`() {
        val a = Quad(Point2(0f, 0f), Point2(10f, 0f), Point2(10f, 10f), Point2(0f, 10f))
        val b = a.map { Point2(it.x + 3f, it.y + 4f) }
        assertEquals(5f, a.distanceTo(b), 0.01f)
    }
}
