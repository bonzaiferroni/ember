package ponder.ember.app.db

import kabinet.utils.startOfDay
import kabinet.utils.today
import kotlinx.datetime.Clock
import ponder.ember.app.AppDao
import ponder.ember.app.ui.entryFormat
import ponder.ember.model.data.Document
import ponder.ember.model.data.DocumentId
import kotlin.time.Duration.Companion.days

class LocalDocumentService(
    private val dao: AppDao
) {
    suspend fun create(): DocumentId {
        val entryNumber = Clock.startOfDay().let { dao.document.readIdsWithinRange(it, it + 1.days) }.size + 1
        val date = Clock.today()
        return Document(
            documentId = DocumentId.random(),
            label = entryFormat(entryNumber, date),
            createdAt = Clock.System.now()
        ).also { dao.document.insert(it.toEntity()) }.documentId
    }
}