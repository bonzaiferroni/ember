package ponder.ember.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import compose.icons.TablerIcons
import compose.icons.tablericons.DatabaseImport
import compose.icons.tablericons.Planet
import compose.icons.tablericons.Plus
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import pondui.ui.behavior.onEnterPressed
import pondui.ui.controls.Button
import pondui.ui.controls.Column
import pondui.ui.controls.ProgressBar
import pondui.ui.controls.Row
import pondui.ui.controls.Scaffold
import pondui.ui.controls.Tab
import pondui.ui.controls.Tabs
import pondui.ui.controls.TextField

@Composable
fun ImportHtmlView(
    viewModel: ImportHtmlModel = viewModel { ImportHtmlModel() }
) {
    val state by viewModel.stateFlow.collectAsState()
    Column(1) {
        Row(1) {
            TextField(
                text = state.url,
                placeholder = "url",
                onValueChange = viewModel::setUrl,
                maxLines = 1,
                modifier = Modifier.weight(1f)
                    .onEnterPressed(viewModel::readUrl)
            )
            Button(TablerIcons.Planet, isEnabled = state.url.startsWith("http"), onClick = viewModel::readUrl)
        }
        Row(1) {
            TextField(
                state.author,
                placeholder = "author",
                onValueChange = viewModel::setAuthor,
                modifier = Modifier.weight(1f)
            )
            TextField(
                state.source,
                placeholder = "source",
                onValueChange = viewModel::setSource,
                modifier = Modifier.weight(1f)
            )
        }
        TextField(
            state.title,
            placeholder = "title",
            onValueChange = viewModel::setTitle,
            modifier = Modifier.fillMaxWidth()
        )
        TextField(
            state.content,
            placeholder = "content",
            onValueChange = viewModel::setContent,
            modifier = Modifier.fillMaxWidth().weight(1f)
        )
        Row(1) {
            Box(modifier = Modifier.weight(1f)) {
                ProgressBar(state.progress, state.work)
            }
            Button(
                TablerIcons.DatabaseImport,
                isEnabled = state.content.isNotBlank() && state.author.isNotBlank(),
                onClick = viewModel::import
            )
        }
    }
}
