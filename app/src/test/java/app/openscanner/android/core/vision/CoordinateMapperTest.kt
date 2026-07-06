package app.openscanner.android.core.vision

import org.junit.Assert.assertEquals
import org.junit.Test

class CoordinateMapperTest {

    @Test
    fun `same aspect ratio maps proportionally`() {
        // 480x640 frame into a 960x1280 view: scale 2, no offset
        val p = CoordinateMapper.mapPoint(Point2(100f, 200f), 480f, 640f, 960f, 1280f)
        assertEquals(200f, p.x, 0.01f)
        assertEquals(400f, p.y, 0.01f)
    }

    @Test
    fun `taller view crops frame horizontally (fill-center)`() {
        // 480x640 frame into 480x960 view: scale = 960/640 = 1.5, x offset = (480-720)/2 = -120
        val p = CoordinateMapper.mapPoint(Point2(240f, 320f), 480f, 640f, 480f, 960f)
        assertEquals(240f, p.x, 0.01f) // frame center stays at view center
        assertEquals(480f, p.y, 0.01f)
    }

    @Test
    fun `wider view crops frame vertically (fill-center)`() {
        // 480x640 frame into 960x640 view: scale = 2, y offset = (640-1280)/2 = -320
        val p = CoordinateMapper.mapPoint(Point2(240f, 320f), 480f, 640f, 960f, 640f)
        assertEquals(480f, p.x, 0.01f)
        assertEquals(320f, p.y, 0.01f)
    }

    @Test
    fun `frame center always maps to view center`() {
        val p = CoordinateMapper.mapPoint(Point2(320f, 240f), 640f, 480f, 1080f, 1920f)
        assertEquals(540f, p.x, 0.01f)
        assertEquals(960f, p.y, 0.01f)
    }
}
