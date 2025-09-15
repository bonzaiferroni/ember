package ponder.ember.app.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.MapColumn
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import ponder.ember.model.data.Block
import ponder.ember.model.data.BlockId
import ponder.ember.model.data.DocumentId

@Dao
interface BlockDao {
    @Insert
    suspend fun insert(block: BlockEntity): Long

    @Insert
    suspend fun insert(blocks: List<BlockEntity>): LongArray

    @Insert
    suspend fun insert(vararg embedding: BlockEmbedding): LongArray

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(block: BlockEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(blocks: List<BlockEntity>): LongArray

    @Update
    suspend fun update(vararg block: BlockEntity): Int

    @Delete
    suspend fun delete(vararg block: BlockEntity): Int

    @Query("DELETE FROM BlockEntity WHERE blockId = :blockId")
    suspend fun deleteById(blockId: BlockId): Int

    @Query("DELETE FROM BlockEntity WHERE blockId IN (:blockIds)")
    suspend fun deleteByIds(blockIds: List<BlockId>): Int

    @Query("SELECT * FROM BlockEntity")
    fun flowAll(): Flow<List<Block>>

    @Query("SELECT * FROM BlockEntity")
    suspend fun readAll(): List<Block>

    @Query("SELECT * FROM BlockEmbedding")
    fun flowAllEmbeddings(): Flow<List<BlockEmbedding>>

    @Query("SELECT blockId, text FROM BlockEntity")
    fun flowAllText(): Flow<Map<@MapColumn("blockId") BlockId, @MapColumn("text") String>>

    @Query("SELECT * FROM BlockEntity WHERE documentId = :documentId")
    fun flowByDocumentId(documentId: DocumentId): Flow<List<Block>>
}