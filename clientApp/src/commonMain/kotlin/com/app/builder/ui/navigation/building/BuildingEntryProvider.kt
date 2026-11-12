package com.app.builder.ui.navigation.building

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.app.builder.domain.gateway.UseCases
import com.app.builder.ui.component.session.SessionStore
import com.app.builder.ui.lifecycle.hostViewModel
import com.app.builder.ui.navigation.LocalRouter
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.navigation.equipment.equipmentFiltersStore
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
            filtersStore = buildingFiltersStore(),
            store = viewModel { BuildingListScreenStore(router = router, state = BuildingListScreenState(), buildingUseCases = useCases.buildingUseCases) }
        )
    }
    entry<Screen.BuildingDetail>(metadata = split()) {
        val router = LocalRouter.current
        BuildingDetailScreen(
            sessionStore = hostViewModel(key = SessionStore.KEY) { SessionStore.get(useCases = useCases) },
            filtersStore = buildingFiltersStore(),
            equipmentFiltersStore = equipmentFiltersStore(),
            store = viewModel {
                BuildingDetailScreenStore(
                    state = BuildingDetailScreenState(),
                    router = router,
                    buildingUseCases = useCases.buildingUseCases,
                    buildingEquipmentUseCases = useCases.buildingEquipmentUseCases,
                    equipmentUseCases = useCases.equipmentUseCases,
                    buildingUuid = it.uuid,
                    new = it.new
                )
            }
        )
    }
}
