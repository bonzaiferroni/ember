package ponder.ember.app.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kabinet.db.TableId
import ponder.ember.model.data.BlockId

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