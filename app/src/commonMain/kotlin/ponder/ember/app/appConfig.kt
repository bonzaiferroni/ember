package ponder.ember.app

import compose.icons.TablerIcons
import compose.icons.tablericons.Edit
import compose.icons.tablericons.Heart
import compose.icons.tablericons.Home
import compose.icons.tablericons.List
import compose.icons.tablericons.Rocket
import kotlinx.collections.immutable.persistentListOf
import ponder.ember.app.ui.ExampleListScreen
import ponder.ember.app.ui.ExampleProfileScreen
import ponder.ember.app.ui.HelloScreen
import ponder.ember.app.ui.StartScreen
import ponder.ember.app.ui.ZenWriterScreen
import ponder.ember.app.ui.JournalScreen
import ponder.ember.app.ui.JournalFeedScreen
import pondui.ui.core.PondConfig
import pondui.ui.core.RouteConfig
import pondui.ui.nav.PortalDoor
import pondui.ui.nav.defaultScreen

val appConfig = PondConfig(
    name = "Ember",
    logo = TablerIcons.Heart,
    home = StartRoute,
    routes = persistentListOf(
        RouteConfig(StartRoute::matchRoute) { defaultScreen<StartRoute> { StartScreen() } },
        RouteConfig(HelloRoute::matchRoute) { defaultScreen<HelloRoute> { HelloScreen() } },
        RouteConfig(ExampleListRoute::matchRoute) { defaultScreen<ExampleListRoute> { ExampleListScreen() } },
        RouteConfig(ExampleProfileRoute::matchRoute) { defaultScreen<ExampleProfileRoute> { ExampleProfileScreen(it) } },
        RouteConfig(WriterRoute::matchRoute) { defaultScreen<WriterRoute> { ZenWriterScreen() } },
        RouteConfig(JournalRoute::matchRoute) { defaultScreen<JournalRoute> { JournalScreen(it) } },
        RouteConfig(JournalFeedRoute::matchRoute) { defaultScreen<JournalFeedRoute> { JournalFeedScreen() } }
    ),
    doors = persistentListOf(
        PortalDoor(TablerIcons.List, JournalFeedRoute, "Feed"),
        PortalDoor(TablerIcons.Edit, JournalRoute()),
        // PortalDoor(TablerIcons.Edit, WriterRoute),
        // PortalDoor(TablerIcons.Rocket, ExampleListRoute),
    ),
)