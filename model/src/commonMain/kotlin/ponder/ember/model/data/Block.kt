package ponder.ember.model.data

import kabinet.db.TableId
import kabinet.utils.randomUuidString
import kotlinx.datetime.Instant
import kotlin.jvm.JvmInline
import kotlinx.serialization.Serializable

@Serializable
data class Block(
    val blockId: BlockId,
    val documentId: DocumentId = DocumentId(""),
    // val label: String?,
    val text: String,
    val position: Int,
    val level: Int?,
    val createdAt: Instant
)

@JvmInline @Serializable
value class BlockId(override val value: String): TableId<String> {
    companion object { fun random() = BlockId(randomUuidString()) }
}

@JvmInline @Serializable
value class ModelId(override val value: String): TableId<String>