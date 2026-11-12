package com.app.builder.ui.navigation.equipment

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.app.builder.domain.gateway.UseCases
import com.app.builder.ui.component.session.SessionStore
import com.app.builder.ui.lifecycle.hostViewModel
import com.app.builder.ui.navigation.LocalRouter
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.navigation.building.buildingFiltersStore
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
            filtersStore = equipmentFiltersStore(),
            store = viewModel { EquipmentListScreenStore(router = router, state = EquipmentListScreenState(), equipmentUseCases = useCases.equipmentUseCases) }
        )
    }
    entry<Screen.EquipmentDetail>(metadata = split()) {
        val router = LocalRouter.current
        EquipmentDetailScreen(
            sessionStore = hostViewModel(key = SessionStore.KEY) { SessionStore.get(useCases = useCases) },
            filtersStore = equipmentFiltersStore(),
            buildingFiltersStore = buildingFiltersStore(),
            store = viewModel {
                EquipmentDetailScreenStore(
                    state = EquipmentDetailScreenState(),
                    router = router,
                    equipmentUseCases = useCases.equipmentUseCases,
                    buildingEquipmentUseCases = useCases.buildingEquipmentUseCases,
                    buildingUseCases = useCases.buildingUseCases,
                    equipmentUuid = it.uuid,
                    new = it.new
                )
            }
        )
    }
}
