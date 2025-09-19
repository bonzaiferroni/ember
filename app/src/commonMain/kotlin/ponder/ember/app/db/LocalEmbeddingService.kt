package ponder.ember.app.db

import kabinet.clients.OllamaClient
import ponder.ember.app.AppDao
import ponder.ember.model.data.Block

class LocalEmbeddingService(
    private val dao: AppDao,
    private val ollama: OllamaClient = OllamaClient()
) {
    suspend fun createFromBlock(block: Block): BlockEmbedding? {
        val level = block.level ?: return null
        if (level != 0) return null
        val embedding = ollama.embed(block.text)?.embeddings?.firstOrNull() ?: return null
        return BlockEmbedding(
            blockId = block.blockId,
            embedding = embedding
        ).also { dao.embedding.insert(it) }
    }
}