package ponder.ember.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import pondui.ui.controls.Button
import pondui.ui.controls.Column
import pondui.ui.controls.Scaffold
import pondui.ui.controls.Section
import pondui.ui.controls.TextField

@Composable
fun ZenWriterScreen(
    viewModel: ZenWriterModel = viewModel { ZenWriterModel() }
) {
    val state by viewModel.stateFlow.collectAsState()
    Scaffold {
        Column(1) {
            Section {
                Writer(
                    text = state.content,
                    onValueChange = viewModel::setContent
                )
            }
//            TextField(
//                text = state.content,
//                onTextChanged = viewModel::setContent,
//                placeholder = "What be on yer mind?",
//                modifier = Modifier.fillMaxWidth()
//            )
//            Row(1) {
//                DropMenu(state.voice, onSelect = viewModel::setVoice)
//                Button("Say", onClick = viewModel::play)
//                Button("Pause", onClick = viewModel::pause)
//            }
            Button("Prompt", onClick = viewModel::prompt)
            TextField(
                text = state.response,
                onTextChanged = { },
                modifier = Modifier.fillMaxWidth()
            )
//            state.image?.let {
//                ByteArrayImage(it)
//            }
        }
    }
}

