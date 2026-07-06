package app.openscanner.android.feature.document

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import app.openscanner.android.OpenScannerApplication
import app.openscanner.android.data.db.PageEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
}
