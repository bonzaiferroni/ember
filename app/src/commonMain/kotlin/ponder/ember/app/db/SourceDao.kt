package ponder.ember.app.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import ponder.ember.model.data.Source
import ponder.ember.model.data.SourceId

@Dao
interface SourceDao {
    @Insert
    suspend fun insert(source: SourceEntity): Long

    @Insert
    suspend fun insert(sources: List<SourceEntity>): LongArray

    @Upsert
    suspend fun upsert(vararg source: SourceEntity): LongArray

    @Update
    suspend fun update(vararg source: SourceEntity): Int

    @Delete
    suspend fun delete(vararg source: SourceEntity): Int

    @Query("DELETE FROM SourceEntity WHERE sourceId = :sourceId")
    suspend fun deleteById(sourceId: SourceId): Int

    @Query("SELECT * FROM SourceEntity")
    fun flowAll(): Flow<List<Source>>

    @Query("SELECT * FROM SourceEntity")
    suspend fun readAll(): List<Source>

    @Query("SELECT * FROM SourceEntity WHERE sourceId = :sourceId")
    fun flowSourceById(sourceId: SourceId): Flow<Source>

    @Query("SELECT * FROM SourceEntity WHERE name = :name")
    suspend fun readByName(name: String): Source?
}
