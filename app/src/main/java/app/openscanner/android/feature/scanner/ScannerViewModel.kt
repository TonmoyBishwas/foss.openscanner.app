package app.openscanner.android.feature.scanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
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
import app.openscanner.android.core.vision.QuadSmoother
import app.openscanner.android.core.vision.rotated
import app.openscanner.android.core.vision.toGrayMat
import java.util.concurrent.Executors
import kotlin.math.hypot
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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

            val detected = detector.detect(upright)
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
                    ScanSession.detectedQuad = detectionAtShutter?.quad?.takeIf {
                        detectionAtShutter.frameWidth > 0 && detectionAtShutter.frameHeight > 0
                    }?.scaled(
                        upright.width.toFloat() / detectionAtShutter.frameWidth,
                        upright.height.toFloat() / detectionAtShutter.frameHeight
                    )

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
