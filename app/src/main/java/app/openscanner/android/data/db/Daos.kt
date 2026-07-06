package app.openscanner.android.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {

    @Query(
        """
        SELECT d.*,
            (SELECT COUNT(*) FROM pages p WHERE p.documentId = d.id) AS pageCount,
            (SELECT p.fileName FROM pages p WHERE p.documentId = d.id
             ORDER BY p.position ASC LIMIT 1) AS firstPageFileName
        FROM documents d
        ORDER BY d.modifiedAt DESC
        """
    )
    fun observeSummaries(): Flow<List<DocumentSummary>>

    @Query("SELECT * FROM documents WHERE id = :id")
    fun observeDocument(id: String): Flow<DocumentEntity?>

    @Insert
    suspend fun insert(document: DocumentEntity)

    @Query("UPDATE documents SET name = :name, modifiedAt = :modifiedAt WHERE id = :id")
    suspend fun rename(id: String, name: String, modifiedAt: Long)

    @Query("UPDATE documents SET modifiedAt = :modifiedAt WHERE id = :id")
    suspend fun touch(id: String, modifiedAt: Long)

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface PageDao {

    @Query("SELECT * FROM pages WHERE documentId = :documentId ORDER BY position ASC")
    fun observePages(documentId: String): Flow<List<PageEntity>>

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM pages WHERE documentId = :documentId")
    suspend fun nextPosition(documentId: String): Int

    @Insert
    suspend fun insert(page: PageEntity)

    @Query("DELETE FROM pages WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM pages WHERE documentId = :documentId ORDER BY position ASC")
    suspend fun pagesOf(documentId: String): List<PageEntity>
}
