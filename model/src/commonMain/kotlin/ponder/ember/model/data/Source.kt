package ponder.ember.model.data

import kabinet.db.TableId
import kabinet.utils.randomUuidString
import kotlinx.datetime.Instant
import kotlin.jvm.JvmInline
import kotlinx.serialization.Serializable

@Serializable
data class Source(
    val sourceId: SourceId,
    val name: String,
    val createdAt: Instant,
)

@JvmInline
@Serializable
value class SourceId(override val value: String) : TableId<String> {
    companion object { fun random() = SourceId(randomUuidString()) }
}
