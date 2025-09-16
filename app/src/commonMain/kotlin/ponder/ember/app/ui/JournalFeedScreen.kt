package ponder.ember.app.ui

import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import ponder.ember.app.JournalRoute
import pondui.ui.controls.LazyScaffold
import pondui.ui.controls.Scaffold
import pondui.ui.controls.Text
import pondui.ui.controls.actionable

@Composable
fun JournalFeedScreen(
    viewModel: JournalFeedModel = viewModel { JournalFeedModel() }
) {
    val state by viewModel.stateFlow.collectAsState()
    LazyScaffold {
        items(state.documents) { document ->
            Text(document.label, modifier = Modifier.actionable(JournalRoute(document.documentId.value)))
        }
    }
}
