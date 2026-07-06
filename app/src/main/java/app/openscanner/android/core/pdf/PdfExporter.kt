package app.openscanner.android.core.pdf

import android.graphics.pdf.PdfDocument
import app.openscanner.android.data.DocumentRepository
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Renders a document's pages into a PDF using the platform PdfDocument —
 * no third-party dependency. Each PDF page adopts its bitmap's pixel size,
 * so pages keep their scanned aspect ratio.
 */
object PdfExporter {

    suspend fun export(
        repository: DocumentRepository,
        documentId: String,
        documentName: String,
        cacheDir: File
    ): File = withContext(Dispatchers.IO) {
        val pages = repository.pagesOf(documentId)
        require(pages.isNotEmpty()) { "Document has no pages" }

        val pdf = PdfDocument()
        try {
            pages.forEachIndexed { index, page ->
                val bitmap = repository.loadPageBitmap(documentId, page.fileName, maxDimension = 2048)
                    ?: return@forEachIndexed
                val info = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, index + 1).create()
                val pdfPage = pdf.startPage(info)
                pdfPage.canvas.drawBitmap(bitmap, 0f, 0f, null)
                pdf.finishPage(pdfPage)
                bitmap.recycle()
            }

            val exportsDir = File(cacheDir, "exports").apply { mkdirs() }
            val safeName = documentName.replace(Regex("[\\\\/:*?\"<>|]"), "_").ifBlank { "scan" }
            val file = File(exportsDir, "$safeName.pdf")
            file.outputStream().use { pdf.writeTo(it) }
            file
        } finally {
            pdf.close()
        }
    }
}
