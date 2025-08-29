package ponder.ember.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import pondui.ui.controls.Button
import pondui.ui.controls.Column
import pondui.ui.controls.DropMenu
import pondui.ui.controls.Scaffold
import pondui.ui.controls.Text
import pondui.ui.controls.TextField

@Composable
fun WriterScreen(
    viewModel: WriterModel = viewModel { WriterModel() }
) {
    val state by viewModel.stateFlow.collectAsState()
    Scaffold {
        Column(1) {
            TextField(
                text = state.content,
                onTextChanged = viewModel::setContent,
                placeholder = "What be on yer mind?",
                modifier = Modifier.fillMaxWidth()
            )
            DropMenu(state.voice, onSelect = viewModel::setVoice)
            Button("Say", onClick = viewModel::play)
        }
    }
}
