package ponder.ember.app.ui

import androidx.lifecycle.viewModelScope
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.copyTo
import io.github.vinceglb.filekit.createDirectories
import io.github.vinceglb.filekit.path
import kabinet.clients.OllamaClient
import kabinet.utils.cosineDistance
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import ponder.ember.app.AppDao
import ponder.ember.app.AppProvider
import ponder.ember.app.AppService
import ponder.ember.app.db.BlockEmbedding
import ponder.ember.app.db.toEntity
import ponder.ember.model.data.Block
import ponder.ember.model.data.BlockId
import ponder.ember.model.data.Document
import ponder.ember.model.data.DocumentId
import pondui.ui.core.ModelState
import pondui.ui.core.StateModel

class JournalModel(
    documentId: DocumentId,
    private val dao: AppDao = AppProvider.dao,
    private val service: AppService = AppProvider.service,
    private val ollama: OllamaClient = OllamaClient()
) : StateModel<JournalState>() {
    override val state = ModelState(JournalState())

    private var otherEmbeddings: List<BlockEmbedding> = emptyList()
    var maxDistance = Float.MIN_VALUE

    private val mutex = Mutex()
    private val distances: MutableMap<BlockId, List<EmbeddingDistance>> = mutableMapOf()

    init {
        var initializedBlocks = false
        ioLaunch {
            launch {
                dao.embedding.readByDocumentId(documentId).forEach { blockEmbedding ->
                    gatherProximityBlocks(blockEmbedding.blockId, blockEmbedding.embedding)
                }
            }

            launch {
                dao.document.flowDocumentById(documentId).collect { document ->
                    setStateFromMain { it.copy(document = document) }
                }
            }

            launch {
                dao.block.flowByDocumentId(documentId).collect { blocks ->
                    val contents = if (!initializedBlocks) {
                        initializedBlocks = true
                        blocks.takeIf { it.isNotEmpty() }?.map { it.text } ?: listOf("")
                    } else stateNow.contents
                    setStateFromMain { state ->
                        state.copy(blocks = blocks.sortedBy { it.position }, contents = contents)
                    }
                }
            }

            launch {
                dao.embedding.flowByNotDocumentId(documentId).collect { embeddings ->
                    otherEmbeddings = embeddings
                }
            }
        }
    }

    fun setLabel(label: String) {
        val document = stateNow.document ?: return
        ioLaunch {
            dao.document.update(document.copy(label = label).toEntity())
        }
    }

    private var contentsJob: Job? = null
    fun setContents(body: WriterBody) {
        val document = stateNow.document ?: return
        val now = Clock.System.now()
        setState { it.copy(contents = body.contents) }

        contentsJob?.cancel()
        contentsJob = ioLaunch {
            delay(5000)

            val newBlocks = mutableListOf<Block>()
            val blocks = body.contents.mapIndexed { index, textBlock ->
                stateNow.blocks.firstOrNull { it.text == textBlock }?.copy(position = index)
                    ?: Block(
                        blockId = BlockId.random(),
                        documentId = document.documentId,
                        text = textBlock,
                        position = index,
                        level = body.blocks[index].markdown.toBlockLevel(),
                        createdAt = now,
                    ).also { newBlocks.add(it)}
            }

            val deletedBlocks = stateNow.blocks.filter { b -> blocks.none { it.blockId == b.blockId } }

            dao.block.deleteByIds(deletedBlocks.map { it.blockId})
            dao.block.upsert(blocks.map { it.toEntity()})

            newBlocks.map { block ->
                async {
                    val level = block.level ?: return@async
                    if (level != 0) return@async
                    val embedding = ollama.embed(block.text)?.embeddings?.firstOrNull() ?: return@async
                    dao.embedding.insert(BlockEmbedding(
                        blockId = block.blockId,
                        embedding = embedding
                    ))
                    gatherProximityBlocks(block.blockId, embedding)
                }
            }.awaitAll()

            deletedBlocks.forEach { block ->
                distances.remove(block.blockId)
            }

            refreshProximities()
        }
    }

    fun setActiveBlock(index: Int) {
        setState { it.copy(activeBlockIndex = index) }
        ioLaunch {
            refreshProximities()
        }
    }

    private suspend fun gatherProximityBlocks(blockId: BlockId, embedding: FloatArray) {
        val blockDistances = otherEmbeddings.mapNotNull { blockEmbedding ->
            val distance = cosineDistance(embedding, blockEmbedding.embedding)
            maxDistance = maxOf(maxDistance, distance)
            val scaledDistance = (distance / maxDistance).takeIf { it < .4f } ?: return@mapNotNull null
            EmbeddingDistance(
                distance = scaledDistance,
                blockId = blockEmbedding.blockId
            )
        }.sortedBy { it.distance }.take(10)
        mutex.withLock {
            distances[blockId] = blockDistances
        }
    }

    private suspend fun refreshProximities() {
        val block = stateNow.blocks.getOrNull(stateNow.activeBlockIndex) ?: return
        val proximityDistances = distances[block.blockId] ?: return
        val blocks = dao.block.readByIds(proximityDistances.map { it.blockId })
        val proximityBlocks = proximityDistances.map {
                pd -> ProximityBlock(pd.distance, blocks.first { it.blockId == pd.blockId})
        }
        setStateFromMain { it.copy(proximityBlocks = proximityBlocks)}
    }

    fun newDocument(block: (DocumentId) -> Unit) {
        viewModelScope.launch {
            block(service.document.create())
        }
    }

    fun saveImage(origin: PlatformFile) {
        val document = stateNow.document ?: return
        ioLaunch {
            val path = "img/${origin.file.name}"
            val directory = PlatformFile("img")
            directory.createDirectories()
            val destination = PlatformFile("${directory.path}/${origin.file.name}")
            origin.copyTo(destination)
            dao.document.update(document.copy(imagePath = path).toEntity())
        }
    }
}

data class JournalState(
    val document: Document? = null,
    val blocks: List<Block> = emptyList(),
    val contents: List<String> = emptyList(),
    val activeBlockIndex: Int = 0,
    val proximityBlocks: List<ProximityBlock> = emptyList()
)

fun entryFormat(entryNumber: Int, date: LocalDate): String {
    val day = date.dayOfMonth
    val month = date.monthNumber
    val year = yearDesignations[date.year]

    val entryPrefix = if (entryNumber == 1) {
        "Entry for The"
    } else {
        "${numberToOrdinalWord(entryNumber)} Entry for The"
    }

    val dayWord = numberToOrdinalWord(day)
    val monthWord = numberToOrdinalWord(month)

    return "$entryPrefix $dayWord Day of The $monthWord Month in The $year"
}

fun numberToOrdinalWord(n: Int): String {
    return ordinals.getOrElse(n - 1) { n.toString() }
}

private val ordinals = listOf(
    "First", "Second", "Third", "Fourth", "Fifth",
    "Sixth", "Seventh", "Eighth", "Ninth", "Tenth",
    "Eleventh", "Twelfth", "Thirteenth", "Fourteenth", "Fifteenth",
    "Sixteenth", "Seventeenth", "Eighteenth", "Nineteenth", "Twentieth",
    "Twenty-First", "Twenty-Second", "Twenty-Third", "Twenty-Fourth", "Twenty-Fifth",
    "Twenty-Sixth", "Twenty-Seventh", "Twenty-Eighth", "Twenty-Ninth", "Thirtieth",
    "Thirty-First"
)

private val yearDesignations = mapOf(
    2025 to "First Year My Petunias Grew Back"
)

private fun MarkdownBlock.toBlockLevel() = when (this) {
    is HeadingBlock -> level
    is ParagraphBlock -> 0
    is QuoteBlock -> null
}

data class EmbeddingDistance(
    val distance: Float,
    val blockId: BlockId,
)

data class ProximityBlock(
    val distance: Float,
    val block: Block
)

const val MAX_PROXIMITY = .5f
