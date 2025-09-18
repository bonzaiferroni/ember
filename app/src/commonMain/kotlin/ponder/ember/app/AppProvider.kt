package ponder.ember.app

import kabinet.utils.Environment
import ponder.ember.app.db.AppDatabase
import ponder.ember.app.db.BlockDao
import ponder.ember.app.db.BlockEmbeddingDao
import ponder.ember.app.db.DocumentDao
import ponder.ember.app.db.LocalDocumentService

object AppProvider {
    var db: AppDatabase? = null
    var env: Environment? = null

    val dao by lazy { AppDao(db ?: error("db not initialized")) }
    val service by lazy { AppService(dao) }
}

class AppDao(db: AppDatabase) {
    val block: BlockDao = db.getBlockDao()
    val document: DocumentDao = db.getDocumentDao()
    val embedding: BlockEmbeddingDao = db.getBlockEmbeddingDao()
}

class AppService(dao: AppDao) {
    val document: LocalDocumentService = LocalDocumentService(dao)
}