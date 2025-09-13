package ponder.ember.app

import androidx.compose.runtime.*
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.readString
import kabinet.utils.Environment
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import ponder.ember.app.db.getDatabaseBuilder
import ponder.ember.app.db.getRoomDatabase
import pondui.CacheFile
import pondui.WatchWindow
import pondui.WindowSize
import pondui.ui.core.ProvideAddressContext

fun main() {
    FileKit.init(appId = "ember")
    AppProvider.db = getRoomDatabase(getDatabaseBuilder())
    runBlocking {
        AppProvider.env = PlatformFile("../.env").readString().let { Environment.fromText(it) }
    }
    application {

        val cacheFlow = CacheFile("appcache.json") { AppCache() }
        val cache by cacheFlow.collectAsState()

        val windowState = WatchWindow(cache.windowSize) {
            cacheFlow.value = cacheFlow.value.copy(windowSize = it)
        }

        ProvideAddressContext(
            initialAddress = cache.address
        ) {
            Window(
                state = windowState,
                onCloseRequest = ::exitApplication,
                title = "App",
                undecorated = true,
            ) {
                App(
                    changeRoute = { cacheFlow.value = cache.copy(address = it.toPath()) },
                    exitApp = ::exitApplication
                )
            }
        }
    }
}

@Serializable
data class AppCache(
    val windowSize: WindowSize = WindowSize(600, 800),
    val address: String? = null
)