package ponder.ember.app

import kabinet.clients.HtmlClient
import kabinet.clients.OllamaClient
import kabinet.utils.Environment
import ponder.ember.app.db.AppDatabase
import ponder.ember.app.db.AuthorDao
import ponder.ember.app.db.BlockDao
import ponder.ember.app.db.BlockEmbeddingDao
import ponder.ember.app.db.DocumentDao
import ponder.ember.app.db.EpubClient
import ponder.ember.app.db.LocalAuthorService
import ponder.ember.app.db.LocalBlockService
import ponder.ember.app.db.LocalDocumentService
import ponder.ember.app.db.LocalEmbeddingService
import ponder.ember.app.db.LocalSourceService
import ponder.ember.app.db.SourceDao

object AppProvider {
    var db: AppDatabase? = null
    var env: Environment? = null

    val client = AppClient()
    val dao by lazy { AppDao(db ?: error("db not initialized")) }
    val service by lazy { AppService(dao, client) }
}

class AppDao(db: AppDatabase) {
    val block: BlockDao = db.getBlockDao()
    val document: DocumentDao = db.getDocumentDao()
    val embedding: BlockEmbeddingDao = db.getBlockEmbeddingDao()
    val source: SourceDao = db.getSourceDao()
    val author: AuthorDao = db.getAuthorDao()
}

class AppService(dao: AppDao, client: AppClient) {
    val document: LocalDocumentService = LocalDocumentService(dao)
    val author: LocalAuthorService = LocalAuthorService(dao)
    val source: LocalSourceService = LocalSourceService(dao)
    val block: LocalBlockService = LocalBlockService(dao)
    val embedding: LocalEmbeddingService = LocalEmbeddingService(dao, client.ollama)
}

class AppClient(
    val ollama: OllamaClient = OllamaClient(),
    val epub: EpubClient = EpubClient(),
    val html: HtmlClient = HtmlClient()
)