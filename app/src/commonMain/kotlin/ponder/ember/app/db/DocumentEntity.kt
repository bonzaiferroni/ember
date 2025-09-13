package ponder.ember.app.db

import androidx.room.PrimaryKey
import kotlinx.datetime.Instant
import ponder.ember.model.data.DocumentId

data class DocumentEntity(
    @PrimaryKey
    val documentId: DocumentId,
    val label: String,
    val createdAt: Instant
)