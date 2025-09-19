package ponder.ember.app.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant
import ponder.ember.model.data.Source
import ponder.ember.model.data.SourceId

@Entity
data class SourceEntity(
    @PrimaryKey
    val sourceId: SourceId,
    val name: String,
    val createdAt: Instant
)

fun Source.toEntity() = SourceEntity(
    sourceId = sourceId,
    name = name,
    createdAt = createdAt,
)
