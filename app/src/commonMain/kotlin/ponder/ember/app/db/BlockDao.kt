package ponder.ember.app.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.MapColumn
import androidx.room.MapInfo
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import ponder.ember.model.data.Block
import ponder.ember.model.data.BlockId

@Dao
interface BlockDao {
    @Insert
    suspend fun insert(vararg block: BlockEntity): LongArray

    @Insert
    suspend fun insert(vararg embedding: BlockEmbedding): LongArray

    @Upsert
    suspend fun upsert(vararg block: BlockEntity): LongArray

    @Update
    suspend fun update(vararg block: BlockEntity): Int

    @Delete
    suspend fun delete(vararg block: BlockEntity): Int

    @Query("DELETE FROM BlockEntity WHERE blockId = :blockId")
    suspend fun deleteById(blockId: BlockId): Int

    @Query("SELECT * FROM BlockEntity")
    fun flowAllBlocks(): Flow<List<Block>>

    @Query("SELECT * FROM BlockEmbedding")
    fun flowAllEmbeddings(): Flow<List<BlockEmbedding>>

    @Query("SELECT blockId, text FROM BlockEntity")
    fun flowAllText(): Flow<Map<@MapColumn("blockId") BlockId, @MapColumn("text") String>>


}