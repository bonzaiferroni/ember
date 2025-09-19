package ponder.ember.app.db

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.parser.Parser
import com.fleeksoft.ksoup.select.Selector.selectFirst
import kabinet.clients.HtmlReader
import java.io.File
import java.io.FileInputStream
import java.util.zip.ZipInputStream

class EpubClient(
    private val reader: HtmlReader = HtmlReader()
) {
    fun read(epub: File): EpubDocument {
        val files = epub.unzipToMemory()
        val contentOpf = files.firstOrNull { it.name.equals("content.opf", true) }
            ?.let { Ksoup.parse(it.text, parser = Parser.xmlParser())}
            ?: error ("content.opf not found")

        val title = contentOpf.readMetadata("title") ?: epub.name
        val author = contentOpf.readMetadata("creator") ?: "Unknown"

        val chapterFiles = contentOpf.selectFirst("manifest")?.children()?.mapNotNull { manifestChild ->
            if (manifestChild.attribute("media-type")?.value != "application/xhtml+xml") return@mapNotNull null
            manifestChild.attribute("href")?.value?.let { filename -> files.firstOrNull { it.name == filename }}
                ?: return@mapNotNull null
        }

        val chapters = chapterFiles?.map { readChapter(it) } ?: emptyList()

        return EpubDocument(
            title = title,
            author = author,
            chapters = chapters
        )
    }

    private fun readChapter(file: MemoryFile): EpubChapter {
        val doc = Ksoup.parse(file.text, parser = Parser.xmlParser())
        val webDoc = reader.read("https://file.com", doc)
        return EpubChapter(
            title = file.name,
            wordCount = webDoc.wordCount,
            contents = webDoc.contents.map { it.text }
        )
    }
}

private fun File.unzipToMemory(): List<MemoryFile> {
    val entries = mutableListOf<MemoryFile>()
    val buffer = ByteArray(1024)

    ZipInputStream(FileInputStream(this)).use { zis ->
        var entry = zis.nextEntry
        while (entry != null) {
            if (!entry.isDirectory) {
                val sb = StringBuilder()
                var len: Int
                while (zis.read(buffer).also { len = it } > 0) {
                    sb.append(String(buffer, 0, len, Charsets.UTF_8))
                }
                entries.add(MemoryFile(entry.name, sb.toString()))
            }
            zis.closeEntry()
            entry = zis.nextEntry
        }
    }
    return entries
}

private data class MemoryFile(
    val name: String,
    val text: String,
)

data class EpubDocument(
    val title: String,
    val author: String,
    val chapters: List<EpubChapter>,
)

data class EpubChapter(
    val title: String,
    val wordCount: Int,
    val contents: List<String>,
)

private fun Document.readMetadata(nsKey: String) = selectFirst("metadata > dc|$nsKey")?.text()

