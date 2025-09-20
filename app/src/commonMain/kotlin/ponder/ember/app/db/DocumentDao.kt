package ponder.ember.app.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant
import ponder.ember.model.data.Document
import ponder.ember.model.data.DocumentId

@Dao
interface DocumentDao {
    @Insert
    suspend fun insert(document: DocumentEntity): Long

    @Insert
    suspend fun insert(documents: List<DocumentEntity>): LongArray

    @Upsert
    suspend fun upsert(vararg document: DocumentEntity): LongArray

    @Update
    suspend fun update(vararg document: DocumentEntity): Int

    @Delete
    suspend fun delete(vararg document: DocumentEntity): Int

    @Query("DELETE FROM DocumentEntity WHERE documentId = :documentId")
    suspend fun deleteById(documentId: DocumentId): Int

    @Query("SELECT * FROM DocumentEntity")
    fun flowAll(): Flow<List<Document>>

    @Query("SELECT * FROM DocumentEntity")
    suspend fun readAll(): List<Document>

    @Query("SELECT * FROM DocumentEntity WHERE authorId IS NULL")
    fun flowAllJournal(): Flow<List<Document>>

    @Query("SELECT * FROM DocumentEntity WHERE authorId IS NULL")
    suspend fun readAllJournal(): List<Document>

    @Query("SELECT * FROM DocumentEntity WHERE documentId = :documentId")
    fun flowDocumentById(documentId: DocumentId): Flow<Document>

    @Query("SELECT documentId FROM DocumentEntity WHERE createdAt BETWEEN :start AND :end ORDER BY createdAt")
    suspend fun readIdsWithinRange(start: Instant, end: Instant): List<DocumentId>
}
