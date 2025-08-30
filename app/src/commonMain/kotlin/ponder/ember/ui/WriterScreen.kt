package ponder.ember.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.loadImageBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Codec
import org.jetbrains.skia.Data
import pondui.ui.controls.Button
import pondui.ui.controls.Column
import pondui.ui.controls.DropMenu
import pondui.ui.controls.Row
import pondui.ui.controls.Scaffold
import pondui.ui.controls.Text
import pondui.ui.controls.TextField
import java.io.ByteArrayInputStream

@Composable
fun WriterScreen(
    viewModel: WriterModel = viewModel { WriterModel() }
) {
    val state by viewModel.stateFlow.collectAsState()
    Scaffold {
        Column(1) {
            WriterBlock(
                text = state.content,
                onValueChange = viewModel::setContent
            )
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

