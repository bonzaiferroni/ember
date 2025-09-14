package ponder.ember.app.ui

import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import compose.icons.TablerIcons
import compose.icons.tablericons.Plus
import pondui.ui.behavior.onEnterPressed
import pondui.ui.controls.Button
import pondui.ui.controls.Column
import pondui.ui.controls.LazyScaffold
import pondui.ui.controls.MoreMenu
import pondui.ui.controls.MoreMenuItem
import pondui.ui.controls.Row
import pondui.ui.controls.Scaffold
import pondui.ui.controls.Text
import pondui.ui.controls.bottomBarSpacerItem
import pondui.ui.controls.topBarSpacerItem

@Composable
fun JournalScreen(
    viewModel: JournalModel = viewModel { JournalModel(null) }
) {
    val state by viewModel.stateFlow.collectAsState()
    LazyScaffold {

        items(state.blocks) { block ->
            BlockField(block.text, onValueChange = { })
        }

        item("content") {
            Column(1) {
                BlockField(state.content, onValueChange = viewModel::setContent, modifier = Modifier.onEnterPressed(viewModel::addBlock))
                Row(1) {
                    Button(TablerIcons.Plus, onClick = viewModel::addBlock)
                    MoreMenu {
                        MoreMenuItem("Import", onClick = viewModel::import)
                    }
                }
            }
        }
    }
}
