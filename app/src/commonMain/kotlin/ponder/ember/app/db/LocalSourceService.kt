package ponder.ember.app.db

import kotlinx.datetime.Clock
import ponder.ember.app.AppDao
import ponder.ember.model.data.Author
import ponder.ember.model.data.AuthorId
import ponder.ember.model.data.Source
import ponder.ember.model.data.SourceId

class LocalSourceService(
    private val dao: AppDao
) {
    suspend fun createOrRead(name: String) = dao.source.readByName(name) ?: Source(
        sourceId = SourceId.random(),
        name = name,
        createdAt = Clock.System.now()
    ).also { dao.source.insert(it.toEntity()) }
}