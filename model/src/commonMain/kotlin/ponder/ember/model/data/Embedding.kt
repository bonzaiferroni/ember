package ponder.ember.model.data

import kabinet.db.TableId
import kabinet.utils.randomUuidString
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Suppress("ArrayInDataClass")
@Serializable
data class Embedding(
    val embeddingId: EmbeddingId,
    val modelId: ModelId,
    val vector: FloatArray,
)

@JvmInline
@Serializable
value class EmbeddingId(override val value: String) : TableId<String> {
    companion object { fun random() = EmbeddingId(randomUuidString()) }
}