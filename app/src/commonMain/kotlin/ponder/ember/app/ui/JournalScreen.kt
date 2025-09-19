package ponder.ember.app.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import compose.icons.TablerIcons
import compose.icons.tablericons.Check
import compose.icons.tablericons.ChevronRight
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.copyTo
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import kabinet.utils.format
import kotlinx.coroutines.launch
import ponder.ember.app.JournalRoute
import ponder.ember.model.data.DocumentId
import pondui.ui.behavior.MagicItem
import pondui.ui.controls.Button
import pondui.ui.controls.Column
import pondui.ui.controls.MoreMenu
import pondui.ui.controls.MoreMenuItem
import pondui.ui.controls.ProgressBar
import pondui.ui.controls.Row
import pondui.ui.controls.Scaffold
import pondui.ui.controls.Section
import pondui.ui.controls.Tab
import pondui.ui.controls.Tabs
import pondui.ui.controls.Text
import pondui.ui.controls.TextField
import pondui.ui.controls.actionable
import pondui.ui.nav.LocalNav

@Composable
fun JournalScreen(
    route: JournalRoute,
    viewModel: JournalModel = viewModel(key = route.documentId) { JournalModel(route.toDocumentId()) }
) {
    val state by viewModel.stateFlow.collectAsState()
    val document = state.document ?: return
    val nav = LocalNav.current

    val launcher = rememberFilePickerLauncher { pf ->
        pf?.let { viewModel.saveImage(pf) }
    }

    Scaffold {
        Row(1, verticalAlignment = Alignment.Top) {
            TextField(document.label, onValueChange = viewModel::setLabel, modifier = Modifier.weight(1f))
            MoreMenu {
                MoreMenuItem("Image", onClick = launcher::launch)
                MoreMenuItem("New Document", onClick = { viewModel.newDocument { nav.go(JournalRoute(it.value)) } })
            }
        }
        Row(1, verticalAlignment = Alignment.Top) {
            Section(modifier = Modifier.weight(1f)) {
                Writer(
                    content = WriterContent(contents = state.contents),
                    onValueChange = viewModel::setContents,
                    onActiveBlockChange = viewModel::setActiveBlock
                ) {
                    document.imagePath?. let {
                        AsyncImage(
                            model = PlatformFile(it),
                            contentDescription = "Image preview",
                            modifier = Modifier.fillMaxWidth(),
                            filterQuality = FilterQuality.None
                        )
                    }
                }
            }
//            Box(modifier = Modifier.weight(1f)) {
//                MagicItem(state.blocks.getOrNull(state.activeBlockIndex), scale = .5f) { block ->
//                    val block = block ?: return@MagicItem
//                    Tabs {
//                        Tab("Proximities") {
//                            state.proximityBlocks.forEach { proxBlock ->
//                                Section {
//                                    Column(1) {
//                                        val proximity = 1 - proxBlock.distance
//                                        ProgressBar(proximity) {
//                                            Text(proximity.format(2))
//                                        }
//                                        Text(
//                                            text = proxBlock.block.text,
//                                            modifier = Modifier.actionable(
//                                                route = JournalRoute(documentId = block.documentId.value),
//                                                icon = PointerIcon.Hand
//                                            )
//                                        )
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }
//            }
        }
    }
}
