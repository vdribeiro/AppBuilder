package com.app.builder.ui.component.navigation

import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Job
import com.app.builder.core.config.ClientFlags
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.domain.EntityType
import com.app.builder.domain.gateway.authentication.AuthenticationUseCases
import com.app.builder.ui.store.Store

/**
 * Store backing the app's navigation bar, resolving the [NavigationRoute]s the current user can open from their permissions.
 * It is shared by every screen showing the navigation bar, so the routes are already resolved when a screen is shown.
 *
 * @param state The initial [NavigationState].
 * @property authenticationUseCases The use cases used to observe the current user and their permissions.
 */
class NavigationStore(
    state: NavigationState,
    private val authenticationUseCases: AuthenticationUseCases
): Store<NavigationState, Unit>(initialState = state) {
    init {
        setup()
    }

    /**
     * Observes the current user's permissions and rebuilds the routes allowed for each entity type.
     *
     * @return The [Job] representing this execution.
     */
    private fun setup(): Job = launch(id = "setup") {
        Telemetry.info(tag = TAG, message = "Setup")

        authenticationUseCases.observeCurrentAccount().observe(id = "current_account") { account ->
            val permissions = account?.permissions ?: return@observe
            val routes = buildList {
                if (ClientFlags.value.tasks && permissions[EntityType.TASK] != null) add(NavigationRoute.TASK)
                if (ClientFlags.value.users && permissions[EntityType.USER] != null) add(NavigationRoute.USER)
                if (ClientFlags.value.registries && permissions[EntityType.REGISTRY] != null) add(NavigationRoute.REGISTRY)
                if (ClientFlags.value.home) add(NavigationRoute.HOME)
                if (listOf(
                        EntityType.CLIENT_FLAG,
                        EntityType.CLIENT_CONFIG,
                        EntityType.SERVER_FLAG,
                        EntityType.SERVER_CONFIG
                    ).any { permissions[it] != null }
                ) add(NavigationRoute.CONFIG)
                if (ClientFlags.value.notifications && permissions[EntityType.NOTIFICATION] != null) add(NavigationRoute.NOTIFICATION)
                if (ClientFlags.value.locationCapture && permissions[EntityType.DEVICE_LOCATION] != null) add(NavigationRoute.DEVICE_LOCATION)
                if (ClientFlags.value.nfc && permissions[EntityType.NFC] != null) add(NavigationRoute.NFC)
                if (ClientFlags.value.camera) add(NavigationRoute.CAMERA)
                if (ClientFlags.value.music) add(NavigationRoute.AUDIO)
                if (ClientFlags.value.deviceFiles) add(NavigationRoute.DEVICE_FILE)
                if (permissions[EntityType.TRANSLATION] != null) add(NavigationRoute.TRANSLATION)
                if (ClientFlags.value.design) addAll(
                    elements = listOf(
                        NavigationRoute.DESIGN_COMPONENTS,
                        NavigationRoute.DESIGN_COLORS,
                        NavigationRoute.DESIGN_SHAPES,
                        NavigationRoute.DESIGN_TYPOGRAPHY
                    )
                )
            }.toPersistentList()

            updateState { it.copy(routes = routes) }
        }

        Telemetry.info(tag = TAG, message = "Setup complete")
    }

    companion object {
        private const val TAG = "NavigationStore"
    }
}
