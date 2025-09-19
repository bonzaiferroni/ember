package ponder.ember.app.ui

import kabinet.clients.HtmlClient
import ponder.ember.app.AppProvider
import pondui.ui.core.ModelState
import pondui.ui.core.StateModel

class ImportModel(
    private val app: AppProvider = AppProvider,
    private val htmlClient: HtmlClient = HtmlClient()
) : StateModel<ImportState>() {
    override val state = ModelState(ImportState())

    fun readUrl() {
        val url = stateNow.url.takeIf { it.isNotBlank() } ?: return
        ioLaunch {
            val document = htmlClient.readUrl(url) ?: return@ioLaunch
            val content = buildString {
                document.title?.let {
                    append("# ")
                    append(it)
                    append("\n\n")
                }
                document.contents.forEach { content ->
                    append(content.text)
                    append("\n\n")
                }
            }
            val author = document.authors?.joinToString(", ")
            setStateFromMain {
                it.copy(
                    content = content,
                    source = document.publisherName ?: "",
                    author = author ?: "",
                    title = document.title ?: ""
                )
            }
        }
    }

    fun setUrl(url: String) { setState { it.copy(url = url) } }
    fun setContent(content: String) { setState { it.copy(content = content) } }
    fun setAuthor(author: String) { setState { it.copy(author = author) } }
    fun setSource(source: String) { setState { it.copy(source = source) } }
    fun setTitle(title: String) { setState { it.copy(title = title) } }

    fun import() {
        val content = stateNow.content.takeIf { it.isNotBlank() } ?: return
        val authorName = stateNow.author.takeIf { it.isNotBlank() } ?: return
        val title = stateNow.title.takeIf { it.isNotBlank() } ?: return
        val sourceName = stateNow.source.takeIf { it.isNotBlank() }
        ioLaunch {
            val author = app.service.author.createOrRead(authorName)
            val source = sourceName?.let { app.service.source.createOrRead(it) }
            val document = app.service.document.create(title, author.authorId, source?.sourceId)
            val blocks = content.split('\n').filter { it.isNotBlank() }.mapIndexed { index, text ->
                app.service.block.create(text, document.documentId, index)
            }
            setStateFromMain { it.copy(work = blocks.size) }
            var progress = 0
            blocks.forEach { block ->
                app.service.embedding.createFromBlock(block)
                progress++
                if (progress > stateNow.progress + 10) {
                    setStateFromMain { it.copy(progress = progress) }
                }
            }
            setStateFromMain { it.copy(progress = 0, work = null)}
        }
    }
}

data class ImportState(
    val url: String = "",
    val content: String = "",
    val author: String = "",
    val source: String = "",
    val title: String = "",
    val progress: Int = 0,
    val work: Int? = null,
)