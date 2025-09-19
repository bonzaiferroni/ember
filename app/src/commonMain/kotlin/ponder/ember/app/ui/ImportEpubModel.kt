package ponder.ember.app.ui

import ponder.ember.app.AppProvider
import ponder.ember.app.db.EpubDocument
import pondui.ui.core.ModelState
import pondui.ui.core.StateModel
import java.io.File

class ImportEpubModel(
    private val app: AppProvider = AppProvider,
) : StateModel<ImportEpubState>() {
    override val state = ModelState(ImportEpubState())

    private var document: EpubDocument? = null

    fun readFile(file: File) {
        setState { it.copy(fileName = file.name) }
        ioLaunch {
            val doc = app.client.epub.read(file).also { document = it }
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
        val authorName = stateNow.author.takeIf { it.isNotBlank() } ?: return
        val title = stateNow.title.takeIf { it.isNotBlank() } ?: return
        ioLaunch {
            val author = app.service.author.createOrRead(authorName)
            val source = app.service.source.createOrRead(title)

            // In the future, chapters will be parsed into blocks. For now, only create the document.
            setStateFromMain { it.copy(progress = 0, work = null) }
        }
    }
}

data class ImportEpubState(
    val fileName: String = "",
    val title: String = "",
    val author: String = "",
    val chapters: List<ChapterInfo> = emptyList(),
    val progress: Int = 0,
    val work: Int? = null,
)

data class ChapterInfo(
    val name: String,
    val wordCount: Int,
)