package com.app.builder.ui.navigation.building

import kotlinx.collections.immutable.toImmutableMap
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.app.builder.data.storage.AppFile
import com.app.builder.domain.Building
import com.app.builder.domain.gateway.UseCases
import com.app.builder.ui.component.filters.FiltersStore
import com.app.builder.ui.component.session.SessionStore
import com.app.builder.ui.lifecycle.hostViewModel
import com.app.builder.ui.navigation.LocalRouter
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.navigation.scene.SplitSceneStrategy.Companion.split

/**
 * The building routes.
 *
 * @param useCases The use cases.
 */
fun EntryProviderScope<NavKey>.buildingProvider(useCases: UseCases) {
    entry<Screen.BuildingList>(metadata = split()) {
        val router = LocalRouter.current
        BuildingListScreen(
            sessionStore = hostViewModel(key = SessionStore.KEY) { SessionStore.get(useCases = useCases) },
            filtersStore = filtersStore(),
            store = viewModel { BuildingListScreenStore(router = router, state = BuildingListScreenState(), buildingUseCases = useCases.buildingUseCases) }
        )
    }
    entry<Screen.BuildingDetail>(metadata = split()) {
        val router = LocalRouter.current
        BuildingDetailScreen(
            sessionStore = hostViewModel(key = SessionStore.KEY) { SessionStore.get(useCases = useCases) },
            filtersStore = filtersStore(),
            store = viewModel { BuildingDetailScreenStore(state = BuildingDetailScreenState(), router = router, buildingUseCases = useCases.buildingUseCases, buildingUuid = it.uuid, new = it.new) }
        )
    }
}

/** The building properties by key, labelled by their translation keys. */
private val properties = Building.Property.entries.associate { entry -> entry.name to entry.translationKey }.toImmutableMap()

/** The building properties shown by default. */
private val visibleProperties = listOf(
    Building.Property.NAME,
    Building.Property.DESCRIPTION,
    Building.Property.ADDRESS,
).map { it.name }

/**
 * Gets the building filters store shared by the navigation host, creating it on first use.
 *
 * @return The building filters store.
 */
@Composable
private fun filtersStore(): FiltersStore = hostViewModel(key = FiltersStore.key(storageFile = AppFile.BuildingPreferences)) {
    FiltersStore.get(
        properties = properties,
        storageFile = AppFile.BuildingPreferences,
        sortProperty = Building.Property.MODIFIED_AT.name,
        sortAscending = false,
        visibleProperties = visibleProperties
    )
}
