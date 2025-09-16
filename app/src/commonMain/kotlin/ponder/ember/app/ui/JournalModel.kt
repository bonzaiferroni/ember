package ponder.ember.app.ui

import kabinet.clients.OllamaClient
import kabinet.utils.startOfDay
import kabinet.utils.today
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
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
    initialDocumentId: DocumentId?,
    val dao: AppDao = AppProvider.dao,
    val ollama: OllamaClient = OllamaClient()
) : StateModel<JournalState>() {
    override val state = ModelState(JournalState())

    private var allEmbeddings: List<BlockEmbedding> = emptyList()
    private var embedding: FloatArray? = null
    private var distances: List<Float> = emptyList()

    init {
        loadDocument(initialDocumentId)
    }

    private var loadJob: Job? = null
    private fun loadDocument(documentId: DocumentId?) {
        var initializedBlocks = false
        loadJob?.cancel()
        loadJob = ioLaunch {
            val documentId = documentId
                ?: Clock.startOfDay().let { dao.document.readIdsWithinRange(it, it + 1.days) }.lastOrNull()
                ?: Document(
                    documentId = DocumentId.random(),
                    label = entryFormat(1, Clock.today()),
                    createdAt = Clock.System.now()
                ).also { dao.document.insert(it.toEntity()) }.documentId

            if (dao.block.readBlockCount(documentId) == 0) {
                dao.block.insert(
                    BlockEntity(
                        blockId = BlockId.random(),
                        documentId = documentId,
                        text = "",
                        position = 0,
                        level = 0,
                    createdAt = Clock.System.now()
                    )
                )
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
                        blocks.map { it.text }
                    } else stateNow.contents
                    setStateFromMain { state ->
                        state.copy(blocks = blocks.sortedBy { it.position }, contents = contents)
                    }
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
    fun setContents(parse: WriterParse) {
        val document = stateNow.document ?: return
        val now = Clock.System.now()
        setState { it.copy(contents = parse.contents) }

        contentsJob?.cancel()
        contentsJob = ioLaunch {
            delay(5000)

            val blocks = parse.contents.mapIndexed { index, textBlock ->
                stateNow.blocks.firstOrNull { it.text == textBlock }?.copy(position = index)?.toEntity()
                    ?: BlockEntity(
                        blockId = BlockId.random(),
                        documentId = document.documentId,
                        text = textBlock,
                        position = index,
                        level = parse.blocks[index].markdown.toBlockLevel(),
                        createdAt = now,
                    )
            }

            dao.block.deleteByIds(stateNow.blocks.filter { b -> blocks.none { it.blockId == b.blockId } }.map { it.blockId})
            dao.block.upsert(blocks)

            // val vector = ollama.embed(value)?.embeddings?.firstOrNull() ?: return@ioLaunch
            // distances = allEmbeddings.map { cosineDistance(vector, it.embedding) }
            // embedding = vector
        }
    }

    fun newDocument() {
        ioLaunch {
            val entryNumber = Clock.startOfDay().let { dao.document.readIdsWithinRange(it, it + 1.days) }.size
            val date = Clock.today()
            val documentId = Document(
                documentId = DocumentId.random(),
                label = entryFormat(entryNumber, date),
                createdAt = Clock.System.now()
            ).also { dao.document.insert(it.toEntity()) }.documentId
            loadDocument(documentId)
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

fun entryFormat(entryNumber: Int, date: LocalDate): String {
    val day = date.dayOfMonth
    val month = date.monthNumber
    val year = yearDesignations[date.year]

    val entryPrefix = if (entryNumber == 1) {
        "Entry for the"
    } else {
        "${numberToOrdinalWord(entryNumber)} Entry for the"
    }

    val dayWord = numberToOrdinalWord(day)
    val monthWord = numberToOrdinalWord(month)

    return "$entryPrefix $dayWord Day of the $monthWord Month in the $year"
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