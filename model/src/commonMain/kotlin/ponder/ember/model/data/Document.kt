package ponder.ember.model.data

import kabinet.db.TableId
import kabinet.utils.randomUuidString
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
data class Document(
    val documentId: DocumentId,
    val label: String,
    val imagePath: String? = null,
    val createdAt: Instant,
)

@JvmInline
@Serializable
value class DocumentId(override val value: String) : TableId<String> {
    companion object { fun random() = DocumentId(randomUuidString()) }
}
