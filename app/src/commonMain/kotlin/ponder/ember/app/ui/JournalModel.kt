package ponder.ember.app.ui

import kabinet.clients.OllamaClient
import kabinet.utils.cosineDistance
import kabinet.utils.startOfDay
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
                    val content = if (!initializedBlocks) {
                        initializedBlocks = true
                        blocks.joinToString("\n") { it.text }
                    } else stateNow.content
                    setStateFromMain { state ->
                        state.copy(blocks = blocks.sortedBy { it.position }, content = content)
                    }
                }
            }
        }
    }

    private var job: Job? = null
    fun setContent(value: String) {
        val document = stateNow.document ?: return
        val now = Clock.System.now()
        setState { it.copy(content = value) }

        job?.cancel()
        job = ioLaunch {
            delay(5000)

            val textBlocks = value.split('\n')
            val blocks = textBlocks.mapIndexed { index, textBlock ->
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

    fun addBlock() {
        val text = stateNow.content.takeIf { it.isNotBlank() } ?: return
        val document = stateNow.document ?: return
        val embedding = embedding ?: return
        ioLaunch {
            val blockId = BlockId.random()
            val now = Clock.System.now()
            dao.block.insert(
                BlockEntity(
                    blockId = blockId,
                    documentId = document.documentId,
                    text = text,
                    position = stateNow.blocks.size,
                    createdAt = now,
                )
            )
            dao.block.insert(
                BlockEmbedding(
                    blockId = blockId,
                    embedding = embedding
                )
            )

            setStateFromMain { it.copy(content = "") }
        }
    }
}

data class JournalState(
    val document: Document? = null,
    val blocks: List<Block> = emptyList(),
    // val tags: List<Tag> = emptyList(),
    val content: String = "",
    // val activeTagId: Set<TagId> = emptySet()
)

@Serializable
data class JournalExport(
    val blocks: List<Block>
)