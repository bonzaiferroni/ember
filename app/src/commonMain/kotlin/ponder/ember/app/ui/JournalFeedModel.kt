package ponder.ember.app.ui

import ponder.ember.app.AppProvider
import ponder.ember.app.db.AppDao
import ponder.ember.model.data.Document
import pondui.ui.core.ModelState
import pondui.ui.core.StateModel

class JournalFeedModel(
    val dao: AppDao = AppProvider.dao,
): StateModel<JournalFeedState>() {
    override val state = ModelState(JournalFeedState())

    init {
        ioCollect(dao.document.flowAll()) { documents ->
            setStateFromMain { it.copy(documents = documents) }
        }
    }
}

data class JournalFeedState(
    val documents: List<Document> = emptyList()
)
