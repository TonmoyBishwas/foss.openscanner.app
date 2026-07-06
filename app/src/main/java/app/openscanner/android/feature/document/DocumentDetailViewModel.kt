package app.openscanner.android.feature.document

import android.app.Application
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import app.openscanner.android.OpenScannerApplication
import app.openscanner.android.core.pdf.PdfExporter
import app.openscanner.android.data.db.PageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DocumentDetailViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    val documentId: String = checkNotNull(savedStateHandle["documentId"])

    private val repository =
        (application as OpenScannerApplication).container.documentRepository

    val document = repository.observeDocument(documentId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val pages = repository.observePages(documentId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun rename(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { repository.rename(documentId, name) }
    }

    fun deleteDocument(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deleteDocument(documentId)
            onDeleted()
        }
    }

    fun deletePage(page: PageEntity) {
        viewModelScope.launch { repository.deletePage(page) }
    }

    suspend fun pageThumbnail(page: PageEntity): Bitmap? =
        repository.loadPageBitmap(page.documentId, page.fileName, maxDimension = 360)

    private val _isExporting = MutableStateFlow(false)
    val isExporting = _isExporting.asStateFlow()

    /** Exports the document as PDF and opens the system share sheet. */
    fun sharePdf() {
        val doc = document.value ?: return
        if (_isExporting.value) return
        _isExporting.value = true
        val app = getApplication<Application>()
        viewModelScope.launch {
            try {
                val file = PdfExporter.export(repository, documentId, doc.name, app.cacheDir)
                val uri = FileProvider.getUriForFile(app, "${app.packageName}.fileprovider", file)
                val share = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(share, "Share PDF")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                app.startActivity(chooser)
            } catch (e: Exception) {
                Toast.makeText(app, "Couldn't export the PDF.", Toast.LENGTH_SHORT).show()
            } finally {
                _isExporting.value = false
            }
        }
    }

    /** True when direct save to the Downloads folder is available (API 29+). */
    val canSaveToDownloads: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    /** Writes the PDF into the shared Downloads collection. */
    fun savePdfToDownloads() {
        val doc = document.value ?: return
        if (_isExporting.value || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        _isExporting.value = true
        val app = getApplication<Application>()
        viewModelScope.launch {
            try {
                val file = PdfExporter.export(repository, documentId, doc.name, app.cacheDir)
                withContext(Dispatchers.IO) {
                    val values = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, file.name)
                        put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                    }
                    val resolver = app.contentResolver
                    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                        ?: error("MediaStore rejected the file")
                    resolver.openOutputStream(uri)?.use { out ->
                        file.inputStream().use { it.copyTo(out) }
                    }
                }
                Toast.makeText(app, "Saved to Downloads.", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(app, "Couldn't save the PDF.", Toast.LENGTH_SHORT).show()
            } finally {
                _isExporting.value = false
            }
        }
    }
}
