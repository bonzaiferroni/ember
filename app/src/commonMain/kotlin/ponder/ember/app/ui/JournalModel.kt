package ponder.ember.app.ui

import kabinet.clients.OllamaClient
import kabinet.utils.cosineDistance
import kabinet.utils.startOfDay
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import ponder.ember.app.AppProvider
import ponder.ember.app.db.AppDao
import ponder.ember.app.db.BlockEmbedding
import ponder.ember.app.db.BlockEntity
import ponder.ember.app.db.toEntity
import ponder.ember.model.data.Block
import ponder.ember.model.data.BlockId
import ponder.ember.model.data.Document
import ponder.ember.model.data.DocumentId
import pondui.ui.core.ModelState
import pondui.ui.core.StateModel
import kotlin.time.Duration.Companion.days

class JournalModel(
    val documentId: DocumentId?,
    val dao: AppDao = AppProvider.dao,
    val ollama: OllamaClient = OllamaClient()
) : StateModel<JournalState>() {
    override val state = ModelState(JournalState())

    private var allEmbeddings: List<BlockEmbedding> = emptyList()
    private var embedding: FloatArray? = null
    private var distances: List<Float> = emptyList()
    private var initializedBlocks = false

    init {
        ioLaunch {
            val documentId = documentId
                ?: Clock.startOfDay().let { dao.document.readIdsWithinRange(it, it + 1.days) }.lastOrNull()
                ?: Document(
                    documentId = DocumentId.random(),
                    label = "Journal",
                    createdAt = Clock.System.now()
                ).also { dao.document.insert(it.toEntity()) }.documentId

            launch {
                dao.document.flowDocumentById(documentId).collect { document ->
                    setStateFromMain { it.copy(document = document) }
                }
            }

            launch {
                dao.block.flowByDocumentId(documentId).collect { blocks ->
                    val contents = if (!initializedBlocks) {
                        initializedBlocks = true
                        blocks.map { it.text }
                    } else stateNow.contents
                    setStateFromMain { state ->
                        state.copy(blocks = blocks.sortedBy { it.position }, contents = contents)
                    }
                }
            }
        }
    }

    private var job: Job? = null
    fun setContents(contents: List<String>) {
        val document = stateNow.document ?: return
        val now = Clock.System.now()
        setState { it.copy(contents = contents) }

        job?.cancel()
        job = ioLaunch {
            delay(5000)

            val blocks = contents.mapIndexed { index, textBlock ->
                stateNow.blocks.firstOrNull { it.text == textBlock }?.copy(position = index)?.toEntity()
                    ?: BlockEntity(
                        blockId = BlockId.random(),
                        documentId = document.documentId,
                        text = textBlock,
                        position = index,
                        createdAt = now
                    )
            }

            dao.block.deleteByIds(stateNow.blocks.filter { b -> blocks.none { it.blockId == b.blockId } }.map { it.blockId})
            dao.block.upsert(blocks)

            // val vector = ollama.embed(value)?.embeddings?.firstOrNull() ?: return@ioLaunch
            // distances = allEmbeddings.map { cosineDistance(vector, it.embedding) }
            // embedding = vector
        }
    }
}

data class JournalState(
    val document: Document? = null,
    val blocks: List<Block> = emptyList(),
    // val tags: List<Tag> = emptyList(),
    val contents: List<String> = emptyList(),
    // val activeTagId: Set<TagId> = emptySet()
)

@Serializable
data class JournalExport(
    val blocks: List<Block>
)