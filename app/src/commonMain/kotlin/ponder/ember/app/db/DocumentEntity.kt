package ponder.ember.app.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant
import ponder.ember.model.data.AuthorId
import ponder.ember.model.data.Document
import ponder.ember.model.data.DocumentId
import ponder.ember.model.data.SourceId

@Entity(
    foreignKeys = [
        ForeignKey(AuthorEntity::class, ["authorId"], ["authorId"], ForeignKey.CASCADE),
        ForeignKey(SourceEntity::class, ["sourceId"], ["sourceId"], ForeignKey.CASCADE),
    ],
    indices = [
        Index("authorId"),
        Index("sourceId"),
    ]
)
data class DocumentEntity(
    @PrimaryKey
    val documentId: DocumentId,
    val authorId: AuthorId?,
    val sourceId: SourceId?,
    val label: String,
    val imagePath: String?,
    val createdAt: Instant
)

fun Document.toEntity() = DocumentEntity(
    documentId = documentId,
    authorId = authorId,
    sourceId = sourceId,
    label = label,
    imagePath = imagePath,
    createdAt = createdAt
)