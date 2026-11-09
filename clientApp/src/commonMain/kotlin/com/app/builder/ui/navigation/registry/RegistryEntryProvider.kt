package com.app.builder.ui.navigation.registry

import kotlinx.collections.immutable.toImmutableMap
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.app.builder.data.storage.AppFile
import com.app.builder.domain.Registry
import com.app.builder.domain.gateway.UseCases
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
            filtersStore = filtersStore(),
            store = viewModel { RegistryListScreenStore(router = router, state = RegistryListScreenState(), registryUseCases = useCases.registryUseCases) }
        )
    }
    entry<Screen.RegistryDetail>(metadata = split()) {
        RegistryDetailScreen(
            sessionStore = hostViewModel(key = SessionStore.KEY) { SessionStore.get(useCases = useCases) },
            filtersStore = filtersStore(),
            store = viewModel { RegistryDetailScreenStore(state = RegistryDetailScreenState(), registryUseCases = useCases.registryUseCases, registryUuid = it.uuid) }
        )
    }
}

/** The registry properties by key, labelled by their translation keys. */
private val properties = Registry.Property.entries.associate { entry -> entry.name to entry.translationKey }.toImmutableMap()

/** The registry properties shown by default. */
private val visibleProperties = listOf(
    Registry.Property.REQUEST_TYPE,
    Registry.Property.USER_UUID,
    Registry.Property.ENTITY_TYPE,
).map { it.name }

/**
 * Gets the registry filters store shared by the navigation host, creating it on first use.
 *
 * @return The registry filters store.
 */
@Composable
private fun filtersStore(): FiltersStore = hostViewModel(key = FiltersStore.key(storageFile = AppFile.RegistryPreferences)) {
    FiltersStore.get(
        properties = properties,
        storageFile = AppFile.RegistryPreferences,
        sortProperty = Registry.Property.REQUEST_UTC.name,
        sortAscending = false,
        showDeletedToggle = false,
        visibleProperties = visibleProperties
    )
}
