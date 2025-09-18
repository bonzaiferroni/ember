package ponder.ember.app.db

class AppDao(db: AppDatabase) {
    val block: BlockDao = db.getBlockDao()
    val document: DocumentDao = db.getDocumentDao()
    val embedding: BlockEmbeddingDao = db.getBlockEmbeddingDao()
}