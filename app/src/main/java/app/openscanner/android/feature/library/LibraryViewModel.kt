package app.openscanner.android.feature.library

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.openscanner.android.OpenScannerApplication
import app.openscanner.android.data.db.DocumentSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository =
        (application as OpenScannerApplication).container.documentRepository

    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()

    val documents = combine(repository.observeSummaries(), _query) { docs, q ->
        if (q.isBlank()) docs
        else docs.filter { it.document.name.contains(q.trim(), ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setQuery(value: String) {
        _query.value = value
    }

    fun rename(documentId: String, name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { repository.rename(documentId, name) }
    }

    fun delete(documentId: String) {
        viewModelScope.launch { repository.deleteDocument(documentId) }
    }

    suspend fun thumbnail(summary: DocumentSummary): Bitmap? {
        val fileName = summary.firstPageFileName ?: return null
        return repository.loadPageBitmap(summary.document.id, fileName, maxDimension = 128)
    }
}
