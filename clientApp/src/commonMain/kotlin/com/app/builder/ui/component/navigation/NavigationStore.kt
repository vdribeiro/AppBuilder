package com.app.builder.ui.component.navigation

import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Job
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AppRegistration
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Task
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Translate
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.domain.EntityType
import com.app.builder.domain.gateway.authentication.AuthenticationUseCases
import com.app.builder.ui.core.navigation.NavigationItem
import com.app.builder.ui.navigation.Router
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.store.Store

/**
 * Store backing the app's navigation bar, building the visible [NavigationItem]s from the current user's permissions.
 *
 * @param state The initial [NavigationState].
 * @property router The router used for navigation.
 * @property authenticationUseCases The use cases used to observe the current user and their permissions.
 */
class NavigationStore(
    state: NavigationState,
    private val router: Router,
    private val authenticationUseCases: AuthenticationUseCases
): Store<NavigationState, Unit>(initialState = state) {
    init {
        setup()
    }

    /** Observes the current user's permissions and rebuilds the navigation items allowed for each entity type. */
    private fun setup(): Job = launch(id = "setup") {
        Telemetry.info(tag = TAG, message = "Setup")

        authenticationUseCases.observeCurrentUser().observe(id = "current_user") { user ->
            val permissions = user?.permissions ?: return@observe
            val items = buildList {
                if (permissions[EntityType.TASK] != null) add(
                    NavigationItem(
                        selected = state.selected == NavigationRoute.TASK,
                        text = "task",
                        icon = Icons.Filled.Task,
                        onClick = { router.navigate(screen = Screen.TaskList, option = Router.NavOption.CLEAR) },
                    )
                )
                if (permissions[EntityType.FILE] != null) add(
                    NavigationItem(
                        selected = state.selected == NavigationRoute.FILE,
                        text = "file",
                        icon = Icons.Filled.FileOpen,
                        onClick = { router.navigate(screen = Screen.FileList, option = Router.NavOption.CLEAR) },
                    )
                )
                if (permissions[EntityType.USER] != null) add(
                    NavigationItem(
                        selected = state.selected == NavigationRoute.USER,
                        text = "user",
                        icon = Icons.Filled.ManageAccounts,
                        onClick = { router.navigate(screen = Screen.UserList, option = Router.NavOption.CLEAR) },
                    )
                )
                if (permissions[EntityType.REGISTRY] != null) add(
                    NavigationItem(
                        selected = state.selected == NavigationRoute.REGISTRY,
                        text = "registry",
                        icon = Icons.Filled.AppRegistration,
                        onClick = { router.navigate(screen = Screen.RegistryList, option = Router.NavOption.CLEAR) },
                    )
                )
                if (permissions[EntityType.DEVICE_LOCATION] != null) add(
                    NavigationItem(
                        selected = state.selected == NavigationRoute.DEVICE_LOCATION,
                        text = "device_location",
                        icon = Icons.Filled.GpsFixed,
                        onClick = { router.navigate(screen = Screen.DeviceLocation, option = Router.NavOption.CLEAR) },
                    )
                )
                add(
                    NavigationItem(
                        selected = state.selected == NavigationRoute.NFC,
                        text = "nfc",
                        icon = Icons.Filled.Nfc,
                        onClick = { router.navigate(screen = Screen.NFC, option = Router.NavOption.CLEAR) },
                    )
                )
                add(
                    NavigationItem(
                        selected = state.selected == NavigationRoute.CAMERA,
                        text = "camera",
                        icon = Icons.Filled.Camera,
                        onClick = { router.navigate(screen = Screen.Camera, option = Router.NavOption.CLEAR) },
                    )
                )
                if (permissions[EntityType.NOTIFICATION] != null) add(
                    NavigationItem(
                        selected = state.selected == NavigationRoute.NOTIFICATION,
                        text = "push",
                        icon = Icons.Filled.Notifications,
                        onClick = { router.navigate(screen = Screen.Push, option = Router.NavOption.CLEAR) },
                    )
                )
                if (permissions[EntityType.CLIENT_FLAG] != null) add(
                    NavigationItem(
                        selected = state.selected == NavigationRoute.CLIENT_FLAG,
                        text = "client_flag",
                        icon = Icons.Filled.Flag,
                        onClick = { router.navigate(screen = Screen.ClientFlags, option = Router.NavOption.CLEAR) },
                    )
                )
                if (permissions[EntityType.CLIENT_CONFIG] != null) add(
                    NavigationItem(
                        selected = state.selected == NavigationRoute.CLIENT_CONFIG,
                        text = "client_config",
                        icon = Icons.Filled.Settings,
                        onClick = { router.navigate(screen = Screen.ClientConfigs, option = Router.NavOption.CLEAR) },
                    )
                )
                if (permissions[EntityType.SERVER_FLAG] != null) add(
                    NavigationItem(
                        selected = state.selected == NavigationRoute.SERVER_FLAG,
                        text = "server_flag",
                        icon = Icons.Outlined.Flag,
                        onClick = { router.navigate(screen = Screen.ServerFlags, option = Router.NavOption.CLEAR) },
                    )
                )
                if (permissions[EntityType.SERVER_CONFIG] != null) add(
                    NavigationItem(
                        selected = state.selected == NavigationRoute.SERVER_CONFIG,
                        text = "server_config",
                        icon = Icons.Outlined.Settings,
                        onClick = { router.navigate(screen = Screen.ServerConfigs, option = Router.NavOption.CLEAR) },
                    )
                )
                if (permissions[EntityType.TRANSLATION] != null) add(
                    NavigationItem(
                        selected = state.selected == NavigationRoute.TRANSLATION,
                        text = "translation",
                        icon = Icons.Outlined.Translate,
                        onClick = { router.navigate(screen = Screen.ServerConfigs, option = Router.NavOption.CLEAR) },
                    )
                )
            }.toPersistentList()

            updateState { it.copy(items = items) }
        }

        Telemetry.info(tag = TAG, message = "Setup complete")
    }

    companion object {
        private const val TAG = "NavigationStore"
    }
}
