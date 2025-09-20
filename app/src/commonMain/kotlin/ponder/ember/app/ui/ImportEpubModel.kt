package ponder.ember.app.ui

import ponder.ember.app.AppProvider
import ponder.ember.app.db.EpubChapter
import ponder.ember.app.db.EpubDocument
import pondui.ui.core.ModelState
import pondui.ui.core.StateModel
import java.io.File

class ImportEpubModel(
    private val app: AppProvider = AppProvider,
) : StateModel<ImportEpubState>() {
    override val state = ModelState(ImportEpubState())

    private var epubDoc: EpubDocument? = null

    fun readFile(file: File) {
        setState { it.copy(fileName = file.name) }
        ioLaunch {
            val doc = app.client.epub.read(file).also { epubDoc = it }
            setStateFromMain {
                it.copy(
                    title = doc.title,
                    author = doc.author,
                    chapters = doc.chapters.map { ChapterInfo(it.title, it.wordCount) }
                )
            }
        }
    }

    fun setTitle(title: String) { setState { it.copy(title = title) } }
    fun setAuthor(author: String) { setState { it.copy(author = author) } }

    fun import() {
        val service = app.service
        val authorName = stateNow.author.takeIf { it.isNotBlank() } ?: return
        val title = stateNow.title.takeIf { it.isNotBlank() } ?: return
        val chapters = stateNow.chapters.takeIf { it.isNotEmpty() } ?: return
        setState { it.copy(work = chapters.size)}
        ioLaunch {
            val author = service.author.createOrRead(authorName)
            val source = service.source.createOrRead(title)
            val document = service.document.create(title, author.authorId, source.sourceId)
            chapters.forEach { c ->
                val chapter = epubDoc?.chapters?.first { it.title == c.title } ?: error("chapter not found")
                chapter.contents.forEachIndexed { index, content ->
                    val block = service.block.create(content, document.documentId, index)
                    service.embedding.createFromBlock(block)
                }
                setStateFromMain { it.copy(progress = it.progress + 1)}
            }

            // In the future, chapters will be parsed into blocks. For now, only create the document.
            setStateFromMain { it.copy(progress = 0, work = null) }
        }
    }

    fun removeChapter(chapter: ChapterInfo) {
        val selectedChapter = if (stateNow.chapter?.title == chapter.title) null else stateNow.chapter
        setState { it.copy(chapters = it.chapters - chapter, chapter = selectedChapter) }
    }

    fun viewChapter(chapter: ChapterInfo) {
        val doc = epubDoc ?: return
        val selected = doc.chapters.find { it.title == chapter.title } ?: return
        setState { it.copy(chapter = selected) }
    }
}

data class ImportEpubState(
    val fileName: String = "",
    val title: String = "",
    val author: String = "",
    val chapters: List<ChapterInfo> = emptyList(),
    val progress: Int = 0,
    val work: Int? = null,
    val chapter: EpubChapter? = null,
)

data class ChapterInfo(
    val title: String,
    val wordCount: Int,
)