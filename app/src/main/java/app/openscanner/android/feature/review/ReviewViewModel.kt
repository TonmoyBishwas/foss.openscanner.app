package app.openscanner.android.feature.review

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.openscanner.android.OpenScannerApplication
import app.openscanner.android.core.ScanSession
import app.openscanner.android.core.vision.ScanFilter
import app.openscanner.android.core.vision.ScanFilters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReviewViewModel(application: Application) : AndroidViewModel(application) {

    private val repository =
        (application as OpenScannerApplication).container.documentRepository

    private val _selected = MutableStateFlow(
        (application as OpenScannerApplication).container.settingsRepository.defaultFilter.value
    )
    val selected = _selected.asStateFlow()

    /** Full-res preview with the selected filter applied (null while computing). */
    private val _preview = MutableStateFlow<Bitmap?>(null)
    val preview = _preview.asStateFlow()

    private val _thumbnails = MutableStateFlow<Map<ScanFilter, Bitmap>>(emptyMap())
    val thumbnails = _thumbnails.asStateFlow()

    private var base: Bitmap? = null

    fun start() {
        val cropped = ScanSession.cropped ?: return
        if (base === cropped) return
        base = cropped
        recompute()
    }

    fun select(filter: ScanFilter) {
        if (_selected.value == filter) return
        _selected.value = filter
        computePreview()
    }

    fun rotate() {
        val current = base ?: return
        val matrix = Matrix().apply { postRotate(90f) }
        val rotated = Bitmap.createBitmap(current, 0, 0, current.width, current.height, matrix, true)
        base = rotated
        ScanSession.cropped = rotated
        recompute()
    }

    /** The filtered full-res result for saving, falling back to the source. */
    fun result(): Bitmap? = _preview.value ?: base

    private val _isSaving = MutableStateFlow(false)
    val isSaving = _isSaving.asStateFlow()

    /**
     * Persists the filtered page: appends to [ScanSession.targetDocumentId]
     * when set, otherwise creates a new document. Calls [onSaved] with the
     * document id on the main thread.
     */
    fun save(onSaved: (documentId: String, wasAppend: Boolean) -> Unit) {
        val bitmap = result() ?: return
        if (_isSaving.value) return
        _isSaving.value = true
        val target = ScanSession.targetDocumentId
        viewModelScope.launch {
            val documentId = if (target != null) {
                repository.appendPage(target, bitmap)
                target
            } else {
                repository.createDocument(bitmap).id
            }
            ScanSession.clear()
            _isSaving.value = false
            onSaved(documentId, target != null)
        }
    }

    private fun recompute() {
        computePreview()
        computeThumbnails()
    }

    private fun computePreview() {
        val source = base ?: return
        val filter = _selected.value
        _preview.value = null
        viewModelScope.launch {
            val filtered = withContext(Dispatchers.Default) { ScanFilters.apply(source, filter) }
            // Only publish if still current (user may have re-selected or rotated).
            if (_selected.value == filter && base === source) {
                _preview.value = filtered
            }
        }
    }

    private fun computeThumbnails() {
        val source = base ?: return
        viewModelScope.launch {
            val thumbs = withContext(Dispatchers.Default) {
                val scale = 160f / maxOf(source.width, source.height)
                val small = Bitmap.createScaledBitmap(
                    source,
                    (source.width * scale).toInt().coerceAtLeast(1),
                    (source.height * scale).toInt().coerceAtLeast(1),
                    true
                )
                ScanFilter.entries.associateWith { ScanFilters.apply(small, it) }
            }
            if (base === source) _thumbnails.value = thumbs
        }
    }
}
