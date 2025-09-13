package ponder.ember.app.ui

import kabinet.clients.OllamaClient
import kabinet.utils.cosineDistance
import kotlinx.datetime.Clock
import ponder.ember.app.AppProvider
import ponder.ember.app.db.AppDao
import ponder.ember.app.db.BlockEmbedding
import ponder.ember.app.db.BlockEntity
import ponder.ember.model.data.Block
import ponder.ember.model.data.BlockId
import pondui.ui.core.ModelState
import pondui.ui.core.StateModel

class JournalModel(
    val dao: AppDao = AppProvider.appDao,
    val ollama: OllamaClient = OllamaClient()
): StateModel<JournalState>() {
    override val state = ModelState(JournalState())

    private var allBlocks: List<Block> = emptyList()
    private var embeddings: List<BlockEmbedding> = emptyList()
    private var embedding: FloatArray? = null
    private var distances: List<Float> = emptyList()

    init {
        ioCollect(dao.block.flowAllBlocks()) { blocks ->
            allBlocks = blocks
            setState { it.copy(blocks = blocks )}
        }
        ioCollect(dao.block.flowAllEmbeddings()) { embeddings ->
            this.embeddings = embeddings
        }
    }

    fun setContent(value: String) {
        setState { it.copy(content = value) }
        if (value.isBlank()) return
        ioLaunch {
            val vector = ollama.embed(value)?.embeddings?.firstOrNull() ?: return@ioLaunch
            distances = embeddings.map { cosineDistance(vector, it.embedding) }
            embedding = vector
        }
    }

    fun addBlock() {
        val text = stateNow.content.takeIf { it.isNotBlank() } ?: return
        val embedding = embedding ?: return
        ioLaunch {
            val blockId = BlockId.random()
            val now = Clock.System.now()
            dao.block.insert(BlockEntity(
                blockId = blockId,
                text = text,
                createdAt = now
            ))
            dao.block.insert(BlockEmbedding(
                blockId = blockId,
                embedding = embedding
            ))

            setState { it.copy(content = "") }
        }
    }
}

data class JournalState(
    val blocks: List<Block> = emptyList(),
    // val tags: List<Tag> = emptyList(),
    val content: String = "",
    // val activeTagId: Set<TagId> = emptySet()
)