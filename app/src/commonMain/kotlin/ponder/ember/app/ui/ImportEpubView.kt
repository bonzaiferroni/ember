package ponder.ember.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import compose.icons.TablerIcons
import compose.icons.tablericons.DatabaseImport
import compose.icons.tablericons.Plus
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import pondui.ui.controls.Button
import pondui.ui.controls.Column
import pondui.ui.controls.LazyColumn
import pondui.ui.controls.ProgressBar
import pondui.ui.controls.Row
import pondui.ui.controls.Text
import pondui.ui.controls.TextField

@Composable
fun ImportEpubView(
    viewModel: ImportEpubModel = viewModel { ImportEpubModel() }
) {
    val state by viewModel.stateFlow.collectAsState()

    val picker = rememberFilePickerLauncher(
        type = FileKitType.File(extensions = listOf("epub"))
    ) { pf ->
        pf?.let{ viewModel.readFile(pf.file) }
    }

    Column(1) {
        Row(1) {
            Text(
                text = state.fileName,
                modifier = Modifier.weight(1f)
            )
            Button(TablerIcons.Plus, onClick = { picker.launch() })
        }
        Row(1) {
            TextField(
                state.author,
                placeholder = "author",
                onValueChange = viewModel::setAuthor,
                modifier = Modifier.weight(1f)
            )
            TextField(
                state.title,
                placeholder = "title",
                onValueChange = viewModel::setTitle,
                modifier = Modifier.weight(1f)
            )
        }
        LazyColumn(1, modifier = Modifier.weight(1f)) {
            items(state.chapters) { chapter ->
                Row(1) {
                    Text("${chapter.name}: ${chapter.wordCount}")
                }
            }
        }
        Row(1) {
            Box(modifier = Modifier.weight(1f)) {
                ProgressBar(state.progress, state.work)
            }
            Button(
                TablerIcons.DatabaseImport,
                isEnabled = state.title.isNotBlank() && state.author.isNotBlank(),
                onClick = viewModel::import
            )
        }
    }
}
