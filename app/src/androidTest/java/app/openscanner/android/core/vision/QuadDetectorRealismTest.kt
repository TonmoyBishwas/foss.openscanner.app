package app.openscanner.android.core.vision

import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlin.math.hypot
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc

/**
 * Runs the detector against a suite of synthetic "real photo" frames
 * (clutter, shadows, hands, low contrast, dark rooms) with ground-truth
 * corners. Guards the detection quality against regressions: the tuned
 * pipeline scores 33/36; the gate is set slightly below to allow platform
 * jitter, far above the old pipeline's 25/36.
 */
@RunWith(AndroidJUnit4::class)
class QuadDetectorRealismTest {

    companion object {
        @JvmStatic
        @BeforeClass
        fun loadOpenCv() {
            assertTrue("OpenCV failed to load", OpenCVLoader.initLocal())
        }
    }

    @Test
    fun classicalDetectorHandlesRealisticConditions() {
        val classical = QuadDetector()
        runSuite(minHits = 31) { rgba, gray -> classical.detect(gray).also { rgba.release() } }
    }

    @Test
    fun hybridDetectorHandlesRealisticConditions() {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val ml = MlQuadDetector.create(targetContext)
        assertTrue("ML model failed to load from app assets", ml != null)
        val hybrid = HybridQuadDetector(ml)
        runSuite(minHits = 33) { rgba, gray ->
            hybrid.detect(rgba, gray).also { rgba.release() }
        }
    }

    private fun runSuite(minHits: Int, detect: (Mat, Mat) -> Quad?) {
        val assets = InstrumentationRegistry.getInstrumentation().context.assets
        val truth = JSONArray(
            assets.open("detectcases/truth.json").bufferedReader().readText()
        )

        var hits = 0
        var falseQuads = 0
        val failures = StringBuilder()

        for (i in 0 until truth.length()) {
            val case = truth.getJSONObject(i)
            val name = case.getString("name")
            val expected = case.getJSONArray("quad").toQuad()

            val bitmap = assets.open("detectcases/$name.png").use(BitmapFactory::decodeStream)
            val rgba = Mat()
            Utils.bitmapToMat(bitmap, rgba)
            val gray = Mat()
            Imgproc.cvtColor(rgba, gray, Imgproc.COLOR_RGBA2GRAY)

            val diagonal = hypot(gray.width().toFloat(), gray.height().toFloat())
            val detected = detect(rgba, gray)
            gray.release()

            if (detected == null) {
                failures.appendLine("MISS $name")
            } else if (detected.distanceTo(expected) < diagonal * 0.05f) {
                hits++
            } else {
                falseQuads++
                failures.appendLine("BAD  $name (err ${detected.distanceTo(expected)}px)")
            }
        }

        assertTrue(
            "Only $hits/${truth.length()} detections (need >= $minHits), $falseQuads false quads:\n$failures",
            hits >= minHits
        )
        assertTrue("False quads are worse than misses: $falseQuads\n$failures", falseQuads <= 1)
    }

    private fun JSONArray.toQuad(): Quad {
        val points = (0 until 4).map {
            val p = getJSONArray(it)
            Point2(p.getDouble(0).toFloat(), p.getDouble(1).toFloat())
        }
        return Quad.fromUnordered(points)
    }
}
