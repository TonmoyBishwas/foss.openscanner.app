package app.openscanner.android.feature.scanner

import android.content.Context
import android.util.Size
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
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

class ScannerViewModel : ViewModel() {

    /** A detection in upright analysis-frame pixel coordinates. */
    data class Detection(val quad: Quad?, val frameWidth: Int, val frameHeight: Int)

    private val _surfaceRequest = MutableStateFlow<SurfaceRequest?>(null)
    val surfaceRequest = _surfaceRequest.asStateFlow()

    private val _detection = MutableStateFlow<Detection?>(null)
    val detection = _detection.asStateFlow()

    private val _torchOn = MutableStateFlow(false)
    val torchOn = _torchOn.asStateFlow()

    private var camera: Camera? = null
    private val detector = QuadDetector()
    private val smoother = QuadSmoother()
    private val analysisExecutor = Executors.newSingleThreadExecutor()

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
            analysisUseCase
        )
        try {
            awaitCancellation()
        } finally {
            provider.unbindAll()
            camera = null
            smoother.reset()
        }
    }

    fun toggleTorch() {
        val next = !_torchOn.value
        _torchOn.value = next
        camera?.cameraControl?.enableTorch(next)
    }

    override fun onCleared() {
        analysisExecutor.shutdown()
    }
}
