package ponder.ember.app.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant
import ponder.ember.model.data.Document
import ponder.ember.model.data.DocumentId

@Entity
data class DocumentEntity(
    @PrimaryKey
    val documentId: DocumentId,
    val label: String,
    val imagePath: String?,
    val createdAt: Instant
)

fun Document.toEntity() = DocumentEntity(
    documentId = documentId,
    label = label,
    imagePath = imagePath,
    createdAt = createdAt
)