package ponder.ember.app.db

import kotlinx.datetime.Clock
import ponder.ember.app.AppDao
import ponder.ember.model.data.Author
import ponder.ember.model.data.AuthorId

class LocalAuthorService(
    private val dao: AppDao
) {
    suspend fun createOrRead(name: String) = dao.author.readByName(name) ?: Author(
        authorId = AuthorId.random(),
        name = name,
        createdAt = Clock.System.now()
    ).also { dao.author.insert(it.toEntity()) }
}