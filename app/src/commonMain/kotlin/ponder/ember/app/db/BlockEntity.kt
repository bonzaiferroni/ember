package ponder.ember.app.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kabinet.db.TableId
import kotlinx.datetime.Instant
import ponder.ember.model.data.Block
import ponder.ember.model.data.BlockId
import ponder.ember.model.data.DocumentId
import ponder.ember.model.data.TagId

@Entity(
    foreignKeys = [
        ForeignKey(DocumentEntity::class, ["documentId"], ["documentId"], ForeignKey.CASCADE)
    ],
    indices = [
        Index("documentId")
    ]
)
data class BlockEntity(
    @PrimaryKey
    val blockId: BlockId,
    val documentId: DocumentId,
    // val label: String?,
    val text: String,
    val position: Int,
    val level: Int?,
    val createdAt: Instant
)

@Entity(primaryKeys = ["blockId", "tagId"])
data class BlockTagEntity(
    val blockId: BlockId,
    val tagId: TagId,
)

@Suppress("ArrayInDataClass")
@Entity(
    foreignKeys = [
        ForeignKey(BlockEntity::class, ["blockId"], ["blockId"], ForeignKey.CASCADE)
    ],
    indices = [Index("blockId")]
)
data class BlockEmbedding(
    @PrimaryKey(autoGenerate = true)
    val blockEmbeddingId: BlockEmbeddingId = BlockEmbeddingId(0L),
    val blockId: BlockId,
    val embedding: FloatArray,
)

@JvmInline
value class BlockEmbeddingId(override val value: Long): TableId<Long>

fun Block.toEntity() = BlockEntity(
    blockId = blockId,
    documentId = documentId,
    text = text,
    position = position,
    createdAt = createdAt,
    level = level
)