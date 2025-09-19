package ponder.ember.app.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import ponder.ember.model.data.Author
import ponder.ember.model.data.AuthorId

@Dao
interface AuthorDao {
    @Insert
    suspend fun insert(author: AuthorEntity): Long

    @Insert
    suspend fun insert(authors: List<AuthorEntity>): LongArray

    @Upsert
    suspend fun upsert(vararg author: AuthorEntity): LongArray

    @Update
    suspend fun update(vararg author: AuthorEntity): Int

    @Delete
    suspend fun delete(vararg author: AuthorEntity): Int

    @Query("DELETE FROM AuthorEntity WHERE authorId = :authorId")
    suspend fun deleteById(authorId: AuthorId): Int

    @Query("SELECT * FROM AuthorEntity")
    fun flowAll(): Flow<List<Author>>

    @Query("SELECT * FROM AuthorEntity")
    suspend fun readAll(): List<Author>

    @Query("SELECT * FROM AuthorEntity WHERE authorId = :authorId")
    fun flowAuthorById(authorId: AuthorId): Flow<Author>

    @Query("SELECT * FROM AuthorEntity WHERE name = :name")
    suspend fun readByName(name: String): Author?
}
