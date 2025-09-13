package ponder.ember.app

import androidx.compose.runtime.*
import androidx.compose.ui.text.font.FontWeight
import ember.app.generated.resources.Inter_18pt_Regular
import ember.app.generated.resources.Inter_24pt_Light
import ember.app.generated.resources.Inter_28pt_Light
import ember.app.generated.resources.Res
import org.jetbrains.compose.ui.tooling.preview.Preview
import ponder.ember.app.db.AppDao
import ponder.ember.app.db.AppDatabase
import pondui.ui.core.PondApp
import pondui.ui.nav.NavRoute
import pondui.ui.theme.ProvideTheme
import pondui.ui.theme.defaultTheme
import pondui.ui.theme.useFamily

@Composable
@Preview
fun App(
    changeRoute: (NavRoute) -> Unit,
    exitApp: (() -> Unit)?,
) {
    ProvideTheme(
        theme = defaultTheme(
            baseFont = useFamily(Res.font.Inter_18pt_Regular),
            h1Font = useFamily(Res.font.Inter_28pt_Light, FontWeight.Light),
            h2Font = useFamily(Res.font.Inter_24pt_Light, FontWeight.Light),
            h4Font = useFamily(Res.font.Inter_18pt_Regular),
        )
    ) {
        // ProvideUserContext { }
        PondApp(
            config = appConfig,
            changeRoute = changeRoute,
            exitApp = exitApp
        )
    }
}

