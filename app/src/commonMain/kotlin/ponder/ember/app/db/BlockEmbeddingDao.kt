package ponder.ember.app.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ponder.ember.model.data.DocumentId

@Dao
interface BlockEmbeddingDao {
    // Inserts
    @Insert
    suspend fun insert(embedding: BlockEmbedding): Long

    @Insert
    suspend fun insert(embeddings: List<BlockEmbedding>): LongArray

    // Upserts (replace on primary key conflict)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(embedding: BlockEmbedding): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(embeddings: List<BlockEmbedding>): LongArray

    // Updates
    @Update
    suspend fun update(vararg embeddings: BlockEmbedding): Int

    // Deletes
    @Delete
    suspend fun delete(vararg embeddings: BlockEmbedding): Int

    @Query("DELETE FROM BlockEmbedding WHERE blockEmbeddingId = :id")
    suspend fun deleteById(id: BlockEmbeddingId): Int

    @Query("DELETE FROM BlockEmbedding WHERE blockEmbeddingId IN (:ids)")
    suspend fun deleteByIds(ids: List<BlockEmbeddingId>): Int

    // Reads
    @Query("SELECT * FROM BlockEmbedding")
    fun flowAll(): Flow<List<BlockEmbedding>>

    @Query("SELECT * FROM BlockEmbedding")
    suspend fun readAll(): List<BlockEmbedding>

    @Query("SELECT * FROM BlockEmbedding WHERE blockId = :blockId")
    suspend fun readByBlockId(blockId: ponder.ember.model.data.BlockId): List<BlockEmbedding>

    @Query("SELECT be.* FROM BlockEmbedding AS be " +
            "JOIN BlockEntity AS b ON b.blockID = be.blockId " +
            "WHERE b.documentId != :documentId")
    fun flowByNotDocumentId(documentId: DocumentId): Flow<List<BlockEmbedding>>
}
