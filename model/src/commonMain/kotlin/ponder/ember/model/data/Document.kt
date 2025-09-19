package ponder.ember.model.data

import kabinet.db.TableId
import kabinet.utils.randomUuidString
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
data class Document(
    val documentId: DocumentId,
    val authorId: AuthorId? = null,
    val sourceId: SourceId? = null,
    val label: String,
    val imagePath: String?,
    val createdAt: Instant,
)

@JvmInline
@Serializable
value class DocumentId(override val value: String) : TableId<String> {
    companion object { fun random() = DocumentId(randomUuidString()) }
}
