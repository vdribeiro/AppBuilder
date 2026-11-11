package com.app.builder.ui.component.navigation

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.AppRegistration
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.FormatShapes
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsInputComponent
import androidx.compose.material.icons.filled.Task
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
import com.app.builder.core.locale.now
import com.app.builder.core.security.uuid
import com.app.builder.domain.Account
import com.app.builder.domain.EntityType
import com.app.builder.domain.Permission
import com.app.builder.ui.Preview
import com.app.builder.ui.component.session.SessionState
import com.app.builder.ui.core.navigation.NavigationBar
import com.app.builder.ui.core.navigation.NavigationItem
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
    val routes = remember(key1 = session.account) { permittedRoutes(account = session.account) }
    val items = remember(key1 = routes, key2 = selected, key3 = router) {
        navigationItems(routes = routes, selected = selected, router = router)
    }
    if (items.isEmpty()) return
    NavigationBar(
        modifier = Modifier
            .testTag(tag = "navigation_bar")
            .fillMaxWidth(),
        items = items
    )
}

/**
 * Builds the items of the section the [selected] route belongs to.
 *
 * @param routes The routes the current user can open.
 * @param selected The route of the screen showing the bar, or null if it has none.
 * @param router The router used for navigation.
 * @return The items to display.
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
                addSectionEntry(section = NavigationSection.SYSTEM, text = "system", icon = Icons.Filled.Tune)
                addSectionEntry(section = NavigationSection.DESIGN, text = "design", icon = Icons.Filled.Palette)
            }

            NavigationSection.SYSTEM,
            NavigationSection.DESIGN -> {
                add(
                    NavigationItem(
                        text = "app",
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
        NavigationRoute.HOME -> "home"
        NavigationRoute.TASK -> "task"
        NavigationRoute.EQUIPMENT -> "equipment"
        NavigationRoute.BUILDING -> "building"
        NavigationRoute.USER -> "user"
        NavigationRoute.REGISTRY -> "registry"
        NavigationRoute.DEVICE_LOCATION -> "device_location"
        NavigationRoute.NFC -> "nfc"
        NavigationRoute.CAMERA -> "camera"
        NavigationRoute.DEVICE_FILE -> "device_file"
        NavigationRoute.AUDIO -> "audio"
        NavigationRoute.NOTIFICATION -> "push"
        NavigationRoute.CONFIG -> "config"
        NavigationRoute.TRANSLATION -> "translation"
        NavigationRoute.DESIGN_COMPONENTS -> "design_components"
        NavigationRoute.DESIGN_COLORS -> "design_colors"
        NavigationRoute.DESIGN_SHAPES -> "design_shapes"
        NavigationRoute.DESIGN_TYPOGRAPHY -> "design_typography"
    }

/** The icon shown for this route. */
private val NavigationRoute.icon: ImageVector
    get() = when (this) {
        NavigationRoute.HOME -> Icons.Filled.Home
        NavigationRoute.TASK -> Icons.Filled.Task
        NavigationRoute.EQUIPMENT -> Icons.Filled.Build
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
        NavigationRoute.EQUIPMENT -> Screen.EquipmentList
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
        userUuid = uuid(),
        modifiedAt = now(),
        deletedAt = null,
        username = "preview",
        permissions = EntityType.entries.associateWith { Permission.WRITE },
    )
    Navigation(sessionStore = previewStore(initialState = SessionState(loaded = true, account = account)), selected = NavigationRoute.TASK)
}
