package ponder.ember.app.db

import kotlinx.datetime.Clock
import ponder.ember.app.AppDao
import ponder.ember.app.ui.blockLevelOf
import ponder.ember.model.data.Block
import ponder.ember.model.data.BlockId
import ponder.ember.model.data.DocumentId

class LocalBlockService(
    private val dao: AppDao
) {
    suspend fun create(text: String, documentId: DocumentId, position: Int) = Block(
        blockId = BlockId.random(),
        documentId = documentId,
        text = text,
        position = position,
        level = blockLevelOf(text),
        createdAt = Clock.System.now()
    ).also { dao.block.insert(it.toEntity()) }
}