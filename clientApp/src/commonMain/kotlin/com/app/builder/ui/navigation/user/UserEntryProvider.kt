package com.app.builder.ui.navigation.user

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.app.builder.domain.gateway.UseCases
import com.app.builder.ui.component.building.buildingFiltersStore
import com.app.builder.ui.component.building.buildingLinkPickerStore
import com.app.builder.ui.component.equipment.equipmentFiltersStore
import com.app.builder.ui.component.equipment.equipmentLinkPickerStore
import com.app.builder.ui.component.session.SessionStore
import com.app.builder.ui.component.user.userFiltersStore
import com.app.builder.ui.lifecycle.hostViewModel
import com.app.builder.ui.navigation.LocalRouter
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.navigation.scene.SplitSceneStrategy.Companion.split

/**
 * The user routes.
 *
 * @param useCases The use cases.
 */
fun EntryProviderScope<NavKey>.userProvider(useCases: UseCases) {
    entry<Screen.UserList>(metadata = split()) {
        val router = LocalRouter.current
        UserListScreen(
            sessionStore = hostViewModel(key = SessionStore.KEY) { SessionStore.get(useCases = useCases) },
            filtersStore = userFiltersStore(),
            store = viewModel { UserListScreenStore(router = router, state = UserListScreenState(), userUseCases = useCases.userUseCases) }
        )
    }
    entry<Screen.UserDetail>(metadata = split()) {
        val router = LocalRouter.current
        UserDetailScreen(
            sessionStore = hostViewModel(key = SessionStore.KEY) { SessionStore.get(useCases = useCases) },
            filtersStore = userFiltersStore(),
            buildingFiltersStore = buildingFiltersStore(),
            buildingPickerStore = viewModel(key = "building_picker") { buildingLinkPickerStore(buildingUseCases = useCases.buildingUseCases) },
            equipmentFiltersStore = equipmentFiltersStore(),
            equipmentPickerStore = viewModel(key = "equipment_picker") { equipmentLinkPickerStore(equipmentUseCases = useCases.equipmentUseCases) },
            store = viewModel {
                UserDetailScreenStore(
                    state = UserDetailScreenState(),
                    router = router,
                    userUseCases = useCases.userUseCases,
                    accountUseCases = useCases.accountUseCases,
                    buildingUserUseCases = useCases.buildingUserUseCases,
                    equipmentUserUseCases = useCases.equipmentUserUseCases,
                    authenticationUseCases = useCases.authenticationUseCases,
                    userUuid = it.uuid,
                    new = it.new
                )
            }
        )
    }
}
