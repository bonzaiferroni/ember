package ponder.ember.app.ui

import kabinet.clients.OllamaClient
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ponder.ember.app.AppProvider
import ponder.ember.app.db.AppDao
import ponder.ember.app.db.BlockEmbedding
import ponder.ember.model.data.Document
import pondui.ui.core.ModelState
import pondui.ui.core.StateModel

class JournalFeedModel(
    private val dao: AppDao = AppProvider.dao,
    private val ollama: OllamaClient = OllamaClient()
): StateModel<JournalFeedState>() {
    override val state = ModelState(JournalFeedState())

    init {
        ioCollect(dao.document.flowAll()) { documents ->
            setStateFromMain { it.copy(documents = documents) }
        }
    }

    fun refreshEmbeddings() {
        ioLaunch {
            val blocks = dao.block.readNullEmbeddings().takeIf { it.isNotEmpty() } ?: return@ioLaunch
            setStateFromMain { it.copy(count = blocks.size) }
            val mutex = Mutex()
            var progress = 0
            blocks.map { block ->
                async {
                    val embedding = ollama.embed(block.text)?.embeddings?.firstOrNull() ?: return@async
                    dao.embedding.insert(BlockEmbedding(
                        blockId = block.blockId,
                        embedding = embedding
                    ))
                    mutex.withLock {
                        progress++
                        if (progress > stateNow.progress + 10) {
                            setStateFromMain { it.copy(progress = progress)}
                        }
                    }
                }
            }.awaitAll()

            setStateFromMain{ it.copy(progress = 0, count = null) }
        }
    }
}

data class JournalFeedState(
    val documents: List<Document> = emptyList(),
    val progress: Int = 0,
    val count: Int? = null,
)
