package app.openscanner.android.core.vision

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.util.Log
import java.nio.FloatBuffer
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

/**
 * Neural document corner detector: DocAligner lcnet100 heatmap model
 * (Apache-2.0, DocsaidLab/DocAligner), run with ONNX Runtime on CPU.
 *
 * Input: 256x256 BGR, /255, NCHW. Output: (1, 4, 128, 128) heatmap, one
 * channel per corner. A corner is the centroid of the largest blob above
 * threshold in its channel; if any channel has no blob the model isn't
 * confident there's a document, and detect() returns null so the classical
 * detector can take over.
 */
class MlQuadDetector private constructor(
    private val session: OrtSession,
    private val environment: OrtEnvironment
) {

    /**
     * @param rgba upright RGBA frame (any size)
     * @return coarse quad in frame pixel coordinates, or null if not confident
     */
    fun detect(rgba: Mat): Quad? {
        val frameWidth = rgba.width()
        val frameHeight = rgba.height()

        val resized = Mat()
        Imgproc.resize(rgba, resized, Size(INPUT_SIZE.toDouble(), INPUT_SIZE.toDouble()))
        val bgr = Mat()
        Imgproc.cvtColor(resized, bgr, Imgproc.COLOR_RGBA2BGR)
        resized.release()

        val pixels = ByteArray(INPUT_SIZE * INPUT_SIZE * 3)
        bgr.get(0, 0, pixels)
        bgr.release()

        // HWC bytes -> NCHW floats in [0,1]
        val plane = INPUT_SIZE * INPUT_SIZE
        val tensorData = FloatArray(3 * plane)
        for (i in 0 until plane) {
            tensorData[i] = (pixels[i * 3].toInt() and 0xFF) / 255f
            tensorData[plane + i] = (pixels[i * 3 + 1].toInt() and 0xFF) / 255f
            tensorData[2 * plane + i] = (pixels[i * 3 + 2].toInt() and 0xFF) / 255f
        }

        val heatmap: FloatArray = OnnxTensor.createTensor(
            environment,
            FloatBuffer.wrap(tensorData),
            longArrayOf(1, 3, INPUT_SIZE.toLong(), INPUT_SIZE.toLong())
        ).use { input ->
            session.run(mapOf(INPUT_NAME to input)).use { result ->
                val value = result[0].value
                @Suppress("UNCHECKED_CAST")
                val nested = value as Array<Array<Array<FloatArray>>>
                // flatten (1, 4, H, W)
                val hm = nested[0]
                val h = hm[0].size
                val w = hm[0][0].size
                val flat = FloatArray(4 * h * w)
                for (c in 0 until 4) {
                    for (y in 0 until h) {
                        System.arraycopy(hm[c][y], 0, flat, (c * h + y) * w, w)
                    }
                }
                heatmapHeight = h
                heatmapWidth = w
                flat
            }
        }

        val corners = ArrayList<Point2>(4)
        for (channel in 0 until 4) {
            val centroid = largestBlobCentroid(heatmap, channel) ?: return null
            corners.add(
                Point2(
                    centroid.x * frameWidth / heatmapWidth,
                    centroid.y * frameHeight / heatmapHeight
                )
            )
        }
        return Quad.fromUnordered(corners)
    }

    private var heatmapWidth = 128
    private var heatmapHeight = 128

    /** Centroid of the largest connected component above threshold. */
    private fun largestBlobCentroid(heatmap: FloatArray, channel: Int): Point2? {
        val w = heatmapWidth
        val h = heatmapHeight
        val offset = channel * w * h
        val seen = BooleanArray(w * h)
        var bestSize = 0
        var bestSumX = 0L
        var bestSumY = 0L
        val pending = ArrayDeque<Int>()

        for (start in 0 until w * h) {
            if (seen[start] || heatmap[offset + start] < THRESHOLD) continue
            var size = 0
            var sumX = 0L
            var sumY = 0L
            seen[start] = true
            pending.addLast(start)
            while (pending.isNotEmpty()) {
                val index = pending.removeLast()
                size++
                val x = index % w
                val y = index / w
                sumX += x
                sumY += y
                for (neighbor in intArrayOf(index - 1, index + 1, index - w, index + w)) {
                    val valid = when (neighbor) {
                        index - 1 -> x > 0
                        index + 1 -> x < w - 1
                        else -> neighbor in 0 until w * h
                    }
                    if (valid && !seen[neighbor] && heatmap[offset + neighbor] >= THRESHOLD) {
                        seen[neighbor] = true
                        pending.addLast(neighbor)
                    }
                }
            }
            if (size > bestSize) {
                bestSize = size
                bestSumX = sumX
                bestSumY = sumY
            }
        }
        if (bestSize == 0) return null
        return Point2(bestSumX.toFloat() / bestSize, bestSumY.toFloat() / bestSize)
    }

    fun close() {
        session.close()
    }

    companion object {
        private const val INPUT_SIZE = 256
        private const val INPUT_NAME = "img"
        private const val THRESHOLD = 0.3f
        private const val MODEL_ASSET = "models/doc_corners_lcnet100.onnx"

        /** Returns null if the model can't be loaded; callers fall back to classical detection. */
        fun create(context: Context): MlQuadDetector? = try {
            val bytes = context.assets.open(MODEL_ASSET).use { it.readBytes() }
            val environment = OrtEnvironment.getEnvironment()
            val session = environment.createSession(bytes, OrtSession.SessionOptions())
            MlQuadDetector(session, environment)
        } catch (e: Exception) {
            Log.e("OpenScanner", "ML detector unavailable, using classical detection", e)
            null
        }
    }
}
