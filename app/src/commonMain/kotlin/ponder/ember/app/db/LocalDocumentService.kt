package ponder.ember.app.db

import kabinet.utils.startOfDay
import kabinet.utils.today
import kotlinx.datetime.Clock
import ponder.ember.app.AppDao
import ponder.ember.app.ui.entryFormat
import ponder.ember.model.data.AuthorId
import ponder.ember.model.data.Document
import ponder.ember.model.data.DocumentId
import ponder.ember.model.data.SourceId
import kotlin.time.Duration.Companion.days

class LocalDocumentService(
    private val dao: AppDao
) {
    suspend fun createJournalEntry(): Document {
        val entryNumber = Clock.startOfDay().let { dao.document.readIdsWithinRange(it, it + 1.days) }.size + 1
        val date = Clock.today()
        return Document(
            documentId = DocumentId.random(),
            authorId = null,
            sourceId = null,
            label = entryFormat(entryNumber, date),
            imagePath = null,
            createdAt = Clock.System.now()
        ).also { dao.document.insert(it.toEntity()) }
    }

    suspend fun create(
        title: String,
        authorId: AuthorId,
        sourceId: SourceId?,
    ) = Document(
        documentId = DocumentId.random(),
        authorId = authorId,
        sourceId = sourceId,
        label = title,
        imagePath = null,
        createdAt = Clock.System.now()
    ).also { dao.document.insert(it.toEntity()) }
}