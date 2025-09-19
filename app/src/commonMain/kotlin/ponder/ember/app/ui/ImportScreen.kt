package ponder.ember.app.ui

import androidx.compose.runtime.Composable
import pondui.ui.controls.Scaffold
import pondui.ui.controls.Tab
import pondui.ui.controls.Tabs

@Composable
fun ImportScreen() {
    Scaffold {
        Tabs {
            Tab("epub") {
                ImportEpubView()
            }
            Tab("web") {
                ImportHtmlView()
            }
        }
    }
}