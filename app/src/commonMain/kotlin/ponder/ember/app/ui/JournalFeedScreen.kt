package ponder.ember.app.ui

import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import compose.icons.TablerIcons
import compose.icons.tablericons.Plus
import ponder.ember.app.JournalRoute
import pondui.ui.controls.Button
import pondui.ui.controls.LazyScaffold
import pondui.ui.controls.Row
import pondui.ui.controls.Scaffold
import pondui.ui.controls.Text
import pondui.ui.controls.actionable
import pondui.ui.nav.LocalNav

@Composable
fun JournalFeedScreen(
    viewModel: JournalFeedModel = viewModel { JournalFeedModel() }
) {
    val state by viewModel.stateFlow.collectAsState()
    val nav = LocalNav.current
    LazyScaffold {
        item("header") {
            Row(1) {
                Button(TablerIcons.Plus) { nav.go(JournalRoute()) }
            }
        }
        items(state.documents) { document ->
            Text(document.label, modifier = Modifier.actionable(JournalRoute(document.documentId.value)))
        }
    }
}
