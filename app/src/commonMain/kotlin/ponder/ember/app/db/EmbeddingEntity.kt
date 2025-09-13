package ponder.ember.app.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import ponder.ember.model.data.EmbeddingId
import ponder.ember.model.data.ModelId

@Suppress("ArrayInDataClass")
@Entity
data class EmbeddingEntity(
    @PrimaryKey
    val embeddingId: EmbeddingId,
    val modelId: ModelId,
    val vector: FloatArray,
)