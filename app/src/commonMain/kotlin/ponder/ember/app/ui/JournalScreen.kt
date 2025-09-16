package ponder.ember.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import compose.icons.TablerIcons
import compose.icons.tablericons.Check
import ponder.ember.app.JournalRoute
import ponder.ember.model.data.DocumentId
import pondui.ui.controls.Button
import pondui.ui.controls.Row
import pondui.ui.controls.Scaffold
import pondui.ui.controls.Section
import pondui.ui.controls.TextField

@Composable
fun JournalScreen(
    route: JournalRoute,
    viewModel: JournalModel = viewModel {
        JournalModel(
            initialDocumentId = route.documentId.takeIf { it.isNotEmpty() }?.let { DocumentId(it) }
        )
    }
) {
    val state by viewModel.stateFlow.collectAsState()
    Scaffold {
        Row(1) {
            TextField(state.document?.label ?: "", onValueChange = viewModel::setLabel, modifier = Modifier.weight(1f))
            Button(TablerIcons.Check, onClick = viewModel::newDocument)
        }
        Section(modifier = Modifier.weight(1f)) {
            Writer(WriterContent(state.contents), onValueChange = viewModel::setContents)
        }
    }
}
