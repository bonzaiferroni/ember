package ponder.ember.app.ui

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.readString
import io.github.vinceglb.filekit.writeString
import kabinet.clients.OllamaClient
import kabinet.utils.cosineDistance
import kabinet.utils.startOfDay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
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

    private var allBlocks: List<Block> = emptyList()
    private var allDocuments: List<Document> = emptyList()
    private var allEmbeddings: List<BlockEmbedding> = emptyList()
    private var embedding: FloatArray? = null
    private var distances: List<Float> = emptyList()

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
                    setStateFromMain { state -> state.copy(blocks = blocks.sortedBy { it.position }) }
                }
            }
        }
    }

    fun loadDocument(documentId: DocumentId?) {
        ioLaunch {
            val startOfDay = Clock.startOfDay()
            val document = documentId?.let { id -> allDocuments.firstOrNull { it.documentId == id } }
                ?: allDocuments.lastOrNull() { it.label == "Journal" && it.createdAt > startOfDay }
                ?: Document(
                    documentId = DocumentId.random(),
                    label = "Journal",
                    createdAt = Clock.System.now()
                ).also { dao.document.insert(it.toEntity()) }
            val blocks = allBlocks.filter { it.documentId == documentId }
            setStateFromMain { it.copy(document = document, blocks = blocks) }
        }
    }

    fun setContent(value: String) {
        setState { it.copy(content = value) }
        if (value.isBlank()) return
        ioLaunch {
            val vector = ollama.embed(value)?.embeddings?.firstOrNull() ?: return@ioLaunch
            distances = allEmbeddings.map { cosineDistance(vector, it.embedding) }
            embedding = vector
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

            // PlatformFile("export.json").writeString(Json.encodeToString(JournalExport(allBlocks)))

            setStateFromMain { it.copy(content = "") }
        }
    }

    fun import() {
        val document = stateNow.document ?: return
        ioLaunch {
            val export = Json.decodeFromString<JournalExport>(PlatformFile("export.json").readString())
            dao.block.insert(export.blocks.mapIndexed { index, block ->  block.copy(documentId = document.documentId, position = index).toEntity() })
            println("imported ${export.blocks.size}")
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