package com.app.builder.ui.navigation.registry

import kotlinx.collections.immutable.toImmutableMap
import kotlinx.collections.immutable.toPersistentList
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.app.builder.data.storage.AppFile
import com.app.builder.domain.Registry
import com.app.builder.domain.gateway.UseCases
import com.app.builder.ui.component.filters.FiltersState
import com.app.builder.ui.component.filters.FiltersStore
import com.app.builder.ui.component.session.SessionStore
import com.app.builder.ui.lifecycle.hostViewModel
import com.app.builder.ui.navigation.LocalRouter
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.navigation.scene.SplitSceneStrategy.Companion.split

/**
 * The registry routes, which have no add or edit actions since registries are read-only.
 *
 * @param useCases The use cases.
 */
fun EntryProviderScope<NavKey>.registryProvider(useCases: UseCases) {
    entry<Screen.RegistryList>(metadata = split()) {
        val router = LocalRouter.current
        RegistryListScreen(
            sessionStore = hostViewModel(key = SessionStore.KEY) { SessionStore.get(useCases = useCases) },
            filtersStore = hostViewModel(key = FILTERS_KEY) { filtersStore() },
            store = viewModel { RegistryListScreenStore(router = router, state = RegistryListScreenState(), registryUseCases = useCases.registryUseCases) }
        )
    }
    entry<Screen.RegistryDetail>(metadata = split()) {
        RegistryDetailScreen(
            sessionStore = hostViewModel(key = SessionStore.KEY) { SessionStore.get(useCases = useCases) },
            filtersStore = hostViewModel(key = FILTERS_KEY) { filtersStore() },
            store = viewModel { RegistryDetailScreenStore(state = RegistryDetailScreenState(), registryUseCases = useCases.registryUseCases, registryUuid = it.uuid) }
        )
    }
}

/** The key of the registry filters among the stores shared by the navigation host. */
private const val FILTERS_KEY = "registry_filters"
/** The registry properties by name. */
private val properties = Registry.Property.entries.map { it.name }.toPersistentList()
/** The default registry filters. */
private val defaults = AppFile.ActionBarData(
    search = "",
    searchableProperties = properties,
    sortAscending = false,
    sortProperty = Registry.Property.REQUEST_UTC.name,
    visibleProperties = properties,
)

/**
 * Creates the registry filters store.
 *
 * @return The registry filters store.
 */
private fun filtersStore(): FiltersStore = FiltersStore(
    state = FiltersState(
        properties = Registry.Property.entries.associate { entry -> entry.name to entry.translationKey }.toImmutableMap(),
        searchableProperties = properties,
        sortAscending = defaults.sortAscending,
        sortProperty = defaults.sortProperty,
        visibleProperties = properties,
        showDeletedToggle = false
    ),
    storageFile = AppFile.RegistryPreferences,
    defaults = defaults,
)
