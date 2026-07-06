package app.openscanner.android.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import app.openscanner.android.data.db.DocumentEntity
import app.openscanner.android.data.db.DocumentSummary
import app.openscanner.android.data.db.OpenScannerDatabase
import app.openscanner.android.data.db.PageEntity
import java.io.File
import java.text.DateFormat
import java.util.Date
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Documents = Room rows + page JPEGs under filesDir/scans/<documentId>/.
 * Everything stays app-private; nothing touches shared storage until the
 * user explicitly exports.
 */
class DocumentRepository(
    private val context: Context,
    private val database: OpenScannerDatabase
) {
    private val documentDao = database.documentDao()
    private val pageDao = database.pageDao()

    fun observeSummaries(): Flow<List<DocumentSummary>> = documentDao.observeSummaries()

    fun observeDocument(id: String): Flow<DocumentEntity?> = documentDao.observeDocument(id)

    fun observePages(documentId: String): Flow<List<PageEntity>> = pageDao.observePages(documentId)

    suspend fun createDocument(firstPage: Bitmap): DocumentEntity = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val name = "Scan " + DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
            .format(Date(now))
        val document = DocumentEntity(
            id = UUID.randomUUID().toString(),
            name = name,
            createdAt = now,
            modifiedAt = now
        )
        documentDao.insert(document)
        appendPageInternal(document.id, firstPage, now)
        document
    }

    suspend fun appendPage(documentId: String, page: Bitmap) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        appendPageInternal(documentId, page, now)
        documentDao.touch(documentId, now)
    }

    private suspend fun appendPageInternal(documentId: String, page: Bitmap, now: Long) {
        val pageId = UUID.randomUUID().toString()
        val fileName = "$pageId.jpg"
        val file = pageFile(documentId, fileName)
        file.parentFile?.mkdirs()
        file.outputStream().use { page.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        pageDao.insert(
            PageEntity(
                id = pageId,
                documentId = documentId,
                position = pageDao.nextPosition(documentId),
                fileName = fileName,
                createdAt = now
            )
        )
    }

    suspend fun rename(documentId: String, name: String) {
        documentDao.rename(documentId, name.trim(), System.currentTimeMillis())
    }

    suspend fun deleteDocument(documentId: String) = withContext(Dispatchers.IO) {
        documentDao.delete(documentId) // pages cascade
        documentDir(documentId).deleteRecursively()
    }

    suspend fun deletePage(page: PageEntity) = withContext(Dispatchers.IO) {
        pageDao.delete(page.id)
        pageFile(page.documentId, page.fileName).delete()
        documentDao.touch(page.documentId, System.currentTimeMillis())
    }

    suspend fun pagesOf(documentId: String): List<PageEntity> = pageDao.pagesOf(documentId)

    fun pageFile(documentId: String, fileName: String): File =
        File(documentDir(documentId), fileName)

    private fun documentDir(documentId: String): File =
        File(File(context.filesDir, "scans"), documentId)

    /** Decodes a page file downsampled to roughly [maxDimension] on its long side. */
    suspend fun loadPageBitmap(
        documentId: String,
        fileName: String,
        maxDimension: Int
    ): Bitmap? = withContext(Dispatchers.IO) {
        val file = pageFile(documentId, fileName)
        if (!file.exists()) return@withContext null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxDimension) {
            sample *= 2
        }
        BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })
    }
}
