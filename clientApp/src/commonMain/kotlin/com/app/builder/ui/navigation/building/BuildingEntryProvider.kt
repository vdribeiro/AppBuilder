package com.app.builder.ui.navigation.building

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.app.builder.domain.gateway.UseCases
import com.app.builder.ui.component.building.buildingFiltersStore
import com.app.builder.ui.component.equipment.equipmentFiltersStore
import com.app.builder.ui.component.equipment.equipmentLinkPickerStore
import com.app.builder.ui.component.material.materialFiltersStore
import com.app.builder.ui.component.material.materialLinkPickerStore
import com.app.builder.ui.component.session.SessionStore
import com.app.builder.ui.component.user.userFiltersStore
import com.app.builder.ui.component.user.userLinkPickerStore
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
            equipmentPickerStore = viewModel(key = "equipment_picker") { equipmentLinkPickerStore(equipmentUseCases = useCases.equipmentUseCases) },
            materialFiltersStore = materialFiltersStore(),
            materialPickerStore = viewModel(key = "material_picker") { materialLinkPickerStore(materialUseCases = useCases.materialUseCases) },
            userFiltersStore = userFiltersStore(),
            userPickerStore = viewModel(key = "user_picker") { userLinkPickerStore(userUseCases = useCases.userUseCases) },
            store = viewModel {
                BuildingDetailScreenStore(
                    state = BuildingDetailScreenState(),
                    router = router,
                    buildingUseCases = useCases.buildingUseCases,
                    buildingEquipmentUseCases = useCases.buildingEquipmentUseCases,
                    buildingMaterialUseCases = useCases.buildingMaterialUseCases,
                    buildingUserUseCases = useCases.buildingUserUseCases,
                    authenticationUseCases = useCases.authenticationUseCases,
                    buildingUuid = it.uuid,
                    new = it.new
                )
            }
        )
    }
}
