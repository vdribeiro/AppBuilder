package com.app.builder.ui.component.navigation

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.AppRegistration
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.FormatShapes
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsInputComponent
import androidx.compose.material.icons.filled.Task
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.builder.core.config.ClientFlags
import com.app.builder.domain.Account
import com.app.builder.domain.EntityType
import com.app.builder.domain.Permission
import com.app.builder.ui.Preview
import com.app.builder.ui.component.session.SessionState
import com.app.builder.ui.core.navigation.NavigationBar
import com.app.builder.ui.core.navigation.NavigationItem
import com.app.builder.ui.getTranslation
import com.app.builder.ui.navigation.LocalRouter
import com.app.builder.ui.navigation.Router
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.navigation.authenticatedScreen
import com.app.builder.ui.store.Store
import com.app.builder.ui.store.previewStore

/**
 * Renders the app's navigation bar from the routes the current account can open, or nothing while there is no account.
 * The bar shows the [NavigationSection] of the [selected] route. The main section holds the app's domain features plus an item entering each other section,
 * and every other section holds its own routes plus an app item returning to the main section.
 *
 * @param sessionStore The store holding the current user's account, whose permissions decide the routes.
 * @param selected The route of the screen showing the bar, or null if it has none.
 */
@Composable
fun Navigation(
    sessionStore: Store<SessionState, Unit>,
    selected: NavigationRoute? = null,
) {
    val session by sessionStore.stateFlow.collectAsStateWithLifecycle()
    val router = LocalRouter.current
    val flags by ClientFlags.flow.collectAsStateWithLifecycle()
    val routes = remember(key1 = session.account, key2 = flags) { permittedRoutes(account = session.account, flags = flags) }
    val items = remember(key1 = routes, key2 = selected, key3 = router) {
        navigationItems(routes = routes, selected = selected, router = router)
    }
    if (items.isEmpty()) return
    NavigationBar(
        modifier = Modifier
            .testTag(tag = "navigation_bar")
            .fillMaxWidth(),
        moreText = getTranslation(key = "navigation_more"),
        items = items.map { it.copy(text = getTranslation(key = it.text)) }.toPersistentList()
    )
}

/**
 * Builds the items of the section the [selected] route belongs to.
 *
 * @param routes The routes the current user can open.
 * @param selected The route of the screen showing the bar, or null if it has none.
 * @param router The router used for navigation.
 * @return The items to display, with their texts as translation keys.
 */
private fun navigationItems(
    routes: List<NavigationRoute>,
    selected: NavigationRoute?,
    router: Router,
): ImmutableList<NavigationItem> {
    if (routes.isEmpty()) return persistentListOf()
    val section = selected?.section ?: NavigationSection.MAIN

    fun NavigationRoute.toItem() = NavigationItem(
        selected = this == selected,
        text = text,
        icon = icon,
        onClick = { router.navigate(screen = screen, option = Router.NavOption.CLEAR) },
    )

    /** An item entering the given section through its first route, if the user can open any. */
    fun MutableList<NavigationItem>.addSectionEntry(section: NavigationSection, text: String, icon: ImageVector) {
        val first = routes.firstOrNull { it.section == section } ?: return
        add(
            NavigationItem(
                text = text,
                icon = icon,
                onClick = { router.navigate(screen = first.screen, option = Router.NavOption.CLEAR) },
            )
        )
    }

    return buildList {
        when (section) {
            NavigationSection.MAIN -> {
                routes.filter { it.section == NavigationSection.MAIN }.forEach { add(it.toItem()) }
                addSectionEntry(section = NavigationSection.SYSTEM, text = "navigation_system", icon = Icons.Filled.Tune)
                addSectionEntry(section = NavigationSection.DESIGN, text = "navigation_design", icon = Icons.Filled.Palette)
            }

            NavigationSection.SYSTEM,
            NavigationSection.DESIGN -> {
                add(
                    NavigationItem(
                        text = "navigation_app",
                        icon = Icons.Filled.Apps,
                        onClick = { router.navigate(screen = authenticatedScreen, option = Router.NavOption.CLEAR) },
                    )
                )
                routes.filter { it.section == section }.forEach { add(it.toItem()) }
            }
        }
    }.toPersistentList()
}

/** The translation key of the label shown for this route. */
private val NavigationRoute.text: String
    get() = when (this) {
        NavigationRoute.HOME -> "navigation_home"
        NavigationRoute.TASK -> "navigation_task"
        NavigationRoute.TIME_PUNCH -> "navigation_time_punch"
        NavigationRoute.EQUIPMENT -> "navigation_equipment"
        NavigationRoute.MATERIAL -> "navigation_material"
        NavigationRoute.BUILDING -> "navigation_building"
        NavigationRoute.USER -> "navigation_user"
        NavigationRoute.REGISTRY -> "navigation_registry"
        NavigationRoute.DEVICE_LOCATION -> "navigation_device_location"
        NavigationRoute.NFC -> "navigation_nfc"
        NavigationRoute.CAMERA -> "navigation_camera"
        NavigationRoute.DEVICE_FILE -> "navigation_device_file"
        NavigationRoute.AUDIO -> "navigation_audio"
        NavigationRoute.NOTIFICATION -> "navigation_push"
        NavigationRoute.CONFIG -> "navigation_config"
        NavigationRoute.TRANSLATION -> "navigation_translation"
        NavigationRoute.DESIGN_COMPONENTS -> "navigation_design_components"
        NavigationRoute.DESIGN_COLORS -> "navigation_design_colors"
        NavigationRoute.DESIGN_SHAPES -> "navigation_design_shapes"
        NavigationRoute.DESIGN_TYPOGRAPHY -> "navigation_design_typography"
    }

/** The icon shown for this route. */
private val NavigationRoute.icon: ImageVector
    get() = when (this) {
        NavigationRoute.HOME -> Icons.Filled.Home
        NavigationRoute.TASK -> Icons.Filled.Task
        NavigationRoute.TIME_PUNCH -> Icons.Filled.Timer
        NavigationRoute.EQUIPMENT -> Icons.Filled.Build
        NavigationRoute.MATERIAL -> Icons.Filled.Inventory2
        NavigationRoute.BUILDING -> Icons.Filled.Apartment
        NavigationRoute.USER -> Icons.Filled.AccountCircle
        NavigationRoute.REGISTRY -> Icons.Filled.AppRegistration
        NavigationRoute.DEVICE_LOCATION -> Icons.Filled.GpsFixed
        NavigationRoute.NFC -> Icons.Filled.Nfc
        NavigationRoute.CAMERA -> Icons.Filled.Camera
        NavigationRoute.DEVICE_FILE -> Icons.Filled.Folder
        NavigationRoute.AUDIO -> Icons.Filled.MusicNote
        NavigationRoute.NOTIFICATION -> Icons.Filled.Notifications
        NavigationRoute.CONFIG -> Icons.Filled.Settings
        NavigationRoute.TRANSLATION -> Icons.Outlined.Translate
        NavigationRoute.DESIGN_COMPONENTS -> Icons.Filled.SettingsInputComponent
        NavigationRoute.DESIGN_COLORS -> Icons.Filled.Colorize
        NavigationRoute.DESIGN_SHAPES -> Icons.Filled.FormatShapes
        NavigationRoute.DESIGN_TYPOGRAPHY -> Icons.Filled.FontDownload
    }

/** The screen this route opens. */
private val NavigationRoute.screen: Screen
    get() = when (this) {
        NavigationRoute.HOME -> Screen.Home
        NavigationRoute.TASK -> Screen.TaskList
        NavigationRoute.TIME_PUNCH -> Screen.TimePunchList
        NavigationRoute.EQUIPMENT -> Screen.EquipmentList
        NavigationRoute.MATERIAL -> Screen.MaterialList
        NavigationRoute.BUILDING -> Screen.BuildingList
        NavigationRoute.USER -> Screen.UserList
        NavigationRoute.REGISTRY -> Screen.RegistryList
        NavigationRoute.DEVICE_LOCATION -> Screen.DeviceLocation
        NavigationRoute.NFC -> Screen.NFC
        NavigationRoute.CAMERA -> Screen.Camera
        NavigationRoute.DEVICE_FILE -> Screen.DeviceFiles
        NavigationRoute.AUDIO -> Screen.Audio
        NavigationRoute.NOTIFICATION -> Screen.Push
        NavigationRoute.CONFIG -> Screen.Configs
        NavigationRoute.TRANSLATION -> Screen.Translations
        NavigationRoute.DESIGN_COMPONENTS -> Screen.DesignComponents
        NavigationRoute.DESIGN_COLORS -> Screen.DesignColors
        NavigationRoute.DESIGN_SHAPES -> Screen.DesignShapes
        NavigationRoute.DESIGN_TYPOGRAPHY -> Screen.DesignTypography
    }

@PreviewLightDark
@Composable
private fun NavigationPreview() = Preview {
    val account = Account(
        username = "preview",
        permissions = EntityType.entries.associateWith { Permission.WRITE },
    )
    Navigation(sessionStore = previewStore(initialState = SessionState(loaded = true, account = account)), selected = NavigationRoute.TASK)
}
