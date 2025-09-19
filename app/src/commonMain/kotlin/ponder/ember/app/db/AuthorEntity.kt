package ponder.ember.app.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant
import ponder.ember.model.data.Author
import ponder.ember.model.data.AuthorId

@Entity
data class AuthorEntity(
    @PrimaryKey
    val authorId: AuthorId,
    val name: String,
    val createdAt: Instant,
)

fun Author.toEntity() = AuthorEntity(
    authorId = authorId,
    name = name,
    createdAt = createdAt
)
