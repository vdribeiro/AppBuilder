package com.app.builder.ui.navigation.provider

import kotlinx.collections.immutable.toImmutableMap
import kotlinx.collections.immutable.toPersistentList
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.app.builder.data.storage.AppFile
import com.app.builder.domain.EntityType
import com.app.builder.domain.Registry
import com.app.builder.domain.gateway.UseCases
import com.app.builder.ui.component.actionbar.ActionBarState
import com.app.builder.ui.component.actionbar.ActionBarStore
import com.app.builder.ui.component.navigation.NavigationRoute
import com.app.builder.ui.component.navigation.NavigationState
import com.app.builder.ui.component.navigation.NavigationStore
import com.app.builder.ui.navigation.LocalRouter
import com.app.builder.ui.navigation.Router
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.navigation.scene.SplitSceneStrategy.Companion.split
import com.app.builder.ui.screen.registrydetail.RegistryDetailScreen
import com.app.builder.ui.screen.registrydetail.RegistryDetailScreenState
import com.app.builder.ui.screen.registrydetail.RegistryDetailScreenStore
import com.app.builder.ui.screen.registrylist.RegistryListScreen
import com.app.builder.ui.screen.registrylist.RegistryListScreenState
import com.app.builder.ui.screen.registrylist.RegistryListScreenStore

/**
 * The registry routes.
 *
 * @param useCases The use cases.
 */
fun EntryProviderScope<NavKey>.registryProvider(useCases: UseCases) {
    entry<Screen.RegistryList>(metadata = split()) {
        val router = LocalRouter.current
        val store = viewModel { RegistryListScreenStore(router = router, state = RegistryListScreenState(), registryUseCases = useCases.registryUseCases) }
        RegistryListScreen(
            actionBarStore = viewModel { getActionBarStore(router = router, useCases = useCases) },
            navigationStore = viewModel { NavigationStore(state = NavigationState(selected = NavigationRoute.REGISTRY), router = router, authenticationUseCases = useCases.authenticationUseCases) },
            store = store
        )
    }
    entry<Screen.RegistryDetail>(metadata = split()) {
        val router = LocalRouter.current
        val store = viewModel { RegistryDetailScreenStore(state = RegistryDetailScreenState(), registryUseCases = useCases.registryUseCases, registryUuid = it.uuid) }
        RegistryDetailScreen(
            actionBarStore = viewModel { getActionBarStore(router = router, useCases = useCases) },
            navigationStore = viewModel { NavigationStore(state = NavigationState(selected = NavigationRoute.REGISTRY), router = router, authenticationUseCases = useCases.authenticationUseCases) },
            store = store
        )
    }
}

/** The registry properties by name. */
private val properties = Registry.Property.entries.map { it.name }.toPersistentList()
/** The registry property map of name and translation key. */
private val propertyMap = Registry.Property.entries.associate { entry -> entry.name to entry.translationKey }.toImmutableMap()
/** The default action bar data. */
private val defaults = AppFile.ActionBarData(
    search = "",
    searchableProperties = properties,
    sortAscending = false,
    sortProperty = Registry.Property.REQUEST_UTC.name,
    visibleProperties = properties,
)
/** The default action bar state. */
private val actionBarState = ActionBarState(
    properties = propertyMap,
    searchableProperties = properties,
    sortAscending = defaults.sortAscending,
    sortProperty = defaults.sortProperty,
    visibleProperties = properties
)

/**
 * Get the registry action bar store, which has no add or edit actions since registries are read-only.
 *
 * @param router The router.
 * @param useCases The use cases.
 * @return The action bar store.
 */
private fun getActionBarStore(
    router: Router,
    useCases: UseCases,
): ActionBarStore = ActionBarStore(
    state = actionBarState,
    router = router,
    authenticationUseCases = useCases.authenticationUseCases,
    statusUseCases = useCases.statusUseCases,
    storageFile = AppFile.RegistryPreferences,
    defaults = defaults,
    entityType = EntityType.REGISTRY,
)
