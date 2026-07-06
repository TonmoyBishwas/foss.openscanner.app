package app.openscanner.android.feature.scanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Size
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.openscanner.android.core.ScanSession
import app.openscanner.android.core.vision.Quad
import app.openscanner.android.core.vision.QuadDetector
import app.openscanner.android.core.vision.QuadRefiner
import app.openscanner.android.core.vision.QuadSmoother
import app.openscanner.android.core.vision.rotated
import app.openscanner.android.core.vision.toGrayMat
import java.util.concurrent.Executors
import kotlin.math.hypot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc

class ScannerViewModel : ViewModel() {

    /** A detection in upright analysis-frame pixel coordinates. */
    data class Detection(val quad: Quad?, val frameWidth: Int, val frameHeight: Int)

    private val _surfaceRequest = MutableStateFlow<SurfaceRequest?>(null)
    val surfaceRequest = _surfaceRequest.asStateFlow()

    private val _detection = MutableStateFlow<Detection?>(null)
    val detection = _detection.asStateFlow()

    private val _torchOn = MutableStateFlow(false)
    val torchOn = _torchOn.asStateFlow()

    private val _isCapturing = MutableStateFlow(false)
    val isCapturing = _isCapturing.asStateFlow()

    private var camera: Camera? = null
    private val detector = QuadDetector()
    private val smoother = QuadSmoother()
    private val analysisExecutor = Executors.newSingleThreadExecutor()
    private val captureExecutor = Executors.newSingleThreadExecutor()

    private val previewUseCase = Preview.Builder()
        .setResolutionSelector(
            ResolutionSelector.Builder()
                .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                .build()
        )
        .build()
        .apply { setSurfaceProvider { request -> _surfaceRequest.value = request } }

    // Low-res 4:3 frames: plenty for edge detection, cheap enough for real time.
    // Same aspect ratio as the preview so overlay coordinates line up.
    private val analysisUseCase = ImageAnalysis.Builder()
        .setResolutionSelector(
            ResolutionSelector.Builder()
                .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                .setResolutionStrategy(
                    ResolutionStrategy(
                        Size(640, 480),
                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER
                    )
                )
                .build()
        )
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .build()
        .apply { setAnalyzer(analysisExecutor) { frame -> analyze(frame) } }

    // Full-res still, same 4:3 aspect as the analysis stream so the detected
    // quad scales onto the capture with a single uniform factor.
    private val captureUseCase = ImageCapture.Builder()
        .setResolutionSelector(
            ResolutionSelector.Builder()
                .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                .build()
        )
        .build()

    private fun analyze(frame: ImageProxy) {
        frame.use {
            val gray = it.toGrayMat()
            val upright = gray.rotated(it.imageInfo.rotationDegrees)
            if (upright !== gray) gray.release()

            val detected = detector.detect(upright, previous = smoother.current())
            val diagonal = hypot(upright.width().toFloat(), upright.height().toFloat())
            val smoothed = smoother.update(detected, diagonal)

            _detection.value = Detection(smoothed, upright.width(), upright.height())
            upright.release()
        }
    }

    /** Binds the camera for as long as the calling coroutine is active. */
    suspend fun bindToCamera(appContext: Context, lifecycleOwner: LifecycleOwner) {
        val provider = ProcessCameraProvider.awaitInstance(appContext)
        camera = provider.bindToLifecycle(
            lifecycleOwner,
            CameraSelector.DEFAULT_BACK_CAMERA,
            previewUseCase,
            analysisUseCase,
            captureUseCase
        )
        try {
            awaitCancellation()
        } finally {
            provider.unbindAll()
            camera = null
            smoother.reset()
        }
    }

    /**
     * Takes a full-res still, rotates it upright, scales the live-detected quad
     * to capture coordinates, stores both in [ScanSession] and invokes
     * [onCaptured] on the main thread.
     */
    fun capture(onCaptured: () -> Unit, onError: (Throwable) -> Unit) {
        if (_isCapturing.value) return
        _isCapturing.value = true
        val detectionAtShutter = _detection.value

        captureUseCase.takePicture(
            captureExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val bitmap: Bitmap
                    val rotation: Int
                    image.use {
                        rotation = it.imageInfo.rotationDegrees
                        bitmap = it.toBitmap()
                    }
                    val upright = if (rotation != 0) {
                        val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
                        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                    } else bitmap

                    ScanSession.captured = upright
                    val scaledQuad = detectionAtShutter?.quad?.takeIf {
                        detectionAtShutter.frameWidth > 0 && detectionAtShutter.frameHeight > 0
                    }?.scaled(
                        upright.width.toFloat() / detectionAtShutter.frameWidth,
                        upright.height.toFloat() / detectionAtShutter.frameHeight
                    )
                    // Re-snap corners against the full-res capture: residual
                    // error from the low-res analysis frame scales up ~6x
                    // otherwise.
                    ScanSession.detectedQuad = scaledQuad?.let { refineOnBitmap(upright, it) }

                    viewModelScope.launch {
                        _isCapturing.value = false
                        onCaptured()
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    viewModelScope.launch {
                        _isCapturing.value = false
                        onError(exception)
                    }
                }
            }
        )
    }

    /**
     * Imports a gallery image: decodes it (downsampled to a sane size), runs
     * the same quad detection used live, stores everything in [ScanSession].
     */
    fun importImage(context: Context, uri: Uri, onReady: () -> Unit, onError: (Throwable) -> Unit) {
        viewModelScope.launch {
            try {
                val bitmap = withContext(Dispatchers.IO) { decodeScaled(context, uri, maxDimension = 4096) }
                    ?: error("Could not decode image")
                val quad = withContext(Dispatchers.Default) {
                    detectInBitmap(bitmap)?.let { refineOnBitmap(bitmap, it) }
                }
                ScanSession.captured = bitmap
                ScanSession.detectedQuad = quad
                onReady()
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    private fun decodeScaled(context: Context, uri: Uri, maxDimension: Int): Bitmap? {
        val resolver = context.contentResolver
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            val source = android.graphics.ImageDecoder.createSource(resolver, uri)
            return android.graphics.ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                // Software bitmap in ARGB_8888 so OpenCV can read the pixels.
                decoder.allocator = android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE
                val largest = maxOf(info.size.width, info.size.height)
                if (largest > maxDimension) {
                    val scale = maxDimension.toFloat() / largest
                    decoder.setTargetSize(
                        (info.size.width * scale).toInt().coerceAtLeast(1),
                        (info.size.height * scale).toInt().coerceAtLeast(1)
                    )
                }
            }
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) } ?: return null
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxDimension) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    }

    /** Sub-pixel corner snap against the full-resolution image. */
    private fun refineOnBitmap(bitmap: Bitmap, quad: Quad): Quad {
        val rgba = Mat()
        Utils.bitmapToMat(bitmap, rgba)
        val gray = Mat()
        Imgproc.cvtColor(rgba, gray, Imgproc.COLOR_RGBA2GRAY)
        rgba.release()
        val search = (12f * bitmap.width / 640f).coerceIn(12f, 40f)
        val refined = QuadRefiner.refine(gray, quad, searchRadius = search, samplesPerEdge = 40)
        gray.release()
        return refined
    }

    private fun detectInBitmap(bitmap: Bitmap): Quad? {
        // Detect on a ~640px copy for speed, then scale the quad up.
        val scale = (640f / maxOf(bitmap.width, bitmap.height)).coerceAtMost(1f)
        val smallWidth = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val smallHeight = (bitmap.height * scale).toInt().coerceAtLeast(1)
        val small = if (scale < 1f) {
            Bitmap.createScaledBitmap(bitmap, smallWidth, smallHeight, true)
        } else bitmap

        val rgba = Mat()
        Utils.bitmapToMat(small, rgba)
        val gray = Mat()
        Imgproc.cvtColor(rgba, gray, Imgproc.COLOR_RGBA2GRAY)
        rgba.release()
        val quad = detector.detect(gray)
        gray.release()
        if (small !== bitmap) small.recycle()

        return quad?.scaled(
            bitmap.width.toFloat() / smallWidth,
            bitmap.height.toFloat() / smallHeight
        )
    }

    fun toggleTorch() {
        val next = !_torchOn.value
        _torchOn.value = next
        camera?.cameraControl?.enableTorch(next)
    }

    override fun onCleared() {
        analysisExecutor.shutdown()
        captureExecutor.shutdown()
    }
}
