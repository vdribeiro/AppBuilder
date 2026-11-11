package com.app.builder.ui.navigation.equipment

import kotlinx.collections.immutable.toImmutableMap
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.app.builder.data.storage.AppFile
import com.app.builder.domain.Equipment
import com.app.builder.domain.gateway.UseCases
import com.app.builder.ui.component.filters.FiltersStore
import com.app.builder.ui.component.session.SessionStore
import com.app.builder.ui.lifecycle.hostViewModel
import com.app.builder.ui.navigation.LocalRouter
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.navigation.scene.SplitSceneStrategy.Companion.split

/**
 * The equipment routes.
 *
 * @param useCases The use cases.
 */
fun EntryProviderScope<NavKey>.equipmentProvider(useCases: UseCases) {
    entry<Screen.EquipmentList>(metadata = split()) {
        val router = LocalRouter.current
        EquipmentListScreen(
            sessionStore = hostViewModel(key = SessionStore.KEY) { SessionStore.get(useCases = useCases) },
            filtersStore = filtersStore(),
            store = viewModel { EquipmentListScreenStore(router = router, state = EquipmentListScreenState(), equipmentUseCases = useCases.equipmentUseCases) }
        )
    }
    entry<Screen.EquipmentDetail>(metadata = split()) {
        val router = LocalRouter.current
        EquipmentDetailScreen(
            sessionStore = hostViewModel(key = SessionStore.KEY) { SessionStore.get(useCases = useCases) },
            filtersStore = filtersStore(),
            store = viewModel { EquipmentDetailScreenStore(state = EquipmentDetailScreenState(), router = router, equipmentUseCases = useCases.equipmentUseCases, equipmentUuid = it.uuid, new = it.new) }
        )
    }
}

/** The equipment properties by key, labelled by their translation keys. */
private val properties = Equipment.Property.entries.associate { entry -> entry.name to entry.translationKey }.toImmutableMap()

/** The equipment properties shown by default. */
private val visibleProperties = listOf(
    Equipment.Property.NAME,
    Equipment.Property.DESCRIPTION,
    Equipment.Property.STATUS,
).map { it.name }

/**
 * Gets the equipment filters store shared by the navigation host, creating it on first use.
 *
 * @return The equipment filters store.
 */
@Composable
private fun filtersStore(): FiltersStore = hostViewModel(key = FiltersStore.key(storageFile = AppFile.EquipmentPreferences)) {
    FiltersStore.get(
        properties = properties,
        storageFile = AppFile.EquipmentPreferences,
        sortProperty = Equipment.Property.MODIFIED_AT.name,
        sortAscending = false,
        visibleProperties = visibleProperties
    )
}
