package ponder.ember.app.ui

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.createDirectories
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.readString
import io.github.vinceglb.filekit.writeString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import ponder.ember.app.AppDao
import ponder.ember.app.AppProvider
import ponder.ember.app.db.toEntity
import ponder.ember.model.data.Block
import ponder.ember.model.data.Document
import kotlin.reflect.KClass

@OptIn(InternalSerializationApi::class)
class DbBackup(
    private val dao: AppDao = AppProvider.dao
) {

    private val types = listOf(
        BackupType(
            kClass = Document::class,
            flow = dao.document.flowAll(),
            write = { document -> dao.document.insert(document.map{ it.toEntity()}) }) { dao.document.readAll() },
        BackupType(
            kClass = Block::class,
            flow = dao.block.flowAll(),
            write = { blocks -> dao.block.insert(blocks.map { it.toEntity()})}) { dao.block.readAll() },
    )

    fun start() {
        CoroutineScope(Dispatchers.IO).launch {
            PlatformFile(folderName).createDirectories()

            types.forEach { type ->
                launch {
                    type.sync()
                }
                delay(100)
            }
        }
    }
}

@OptIn(InternalSerializationApi::class)
private class BackupType<T: Any>(
    private val kClass: KClass<T>,
    private val flow: Flow<List<T>>,
    private val write: suspend (List<T>) -> Unit,
    private val read: suspend () -> List<T>,
) {
    suspend fun sync() = coroutineScope {
        val className = kClass.simpleName ?: error("class name not found")
        val path = "$folderName/$className.json"
        val serializer: KSerializer<List<T>> = ListSerializer(kClass.serializer())

        var currentItems = read().takeIf { it.isNotEmpty() }
            ?: PlatformFile(path).takeIf { it.exists() }?.readString()?.let { str ->
                val items = json.decodeFromString(serializer, str)
                println("restoring from backup: $className (${items.size})")
                write(items)
                items
            } ?: emptyList()

        flow.collect { items ->
            if (items != currentItems) {
                PlatformFile(path).writeString(json.encodeToString(serializer, items))
                currentItems = items
            }
        }

    }
}

private const val folderName = "backup"

private val json = Json {
    encodeDefaults = true
}