package ponder.ember.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import pondui.ui.controls.Scaffold

@Composable
fun JournalScreen(
    viewModel: JournalModel = viewModel { JournalModel(null) }
) {
    val state by viewModel.stateFlow.collectAsState()
    Scaffold {
        Writer(WriterContent(state.contents), onValueChange = viewModel::setContents)
    }
}
