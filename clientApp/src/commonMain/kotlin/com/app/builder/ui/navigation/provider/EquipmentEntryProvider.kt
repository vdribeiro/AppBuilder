package com.app.builder.ui.navigation.provider

import kotlinx.collections.immutable.toImmutableMap
import kotlinx.collections.immutable.toPersistentList
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.app.builder.data.storage.AppFile
import com.app.builder.domain.EntityType
import com.app.builder.domain.Equipment
import com.app.builder.domain.gateway.UseCases
import com.app.builder.ui.LocalSplitScreen
import com.app.builder.ui.component.actionbar.ActionBarState
import com.app.builder.ui.component.actionbar.ActionBarStore
import com.app.builder.ui.component.bar.ActionBarLayout
import com.app.builder.ui.component.bar.ActionBarMode
import com.app.builder.ui.component.navigation.NavigationRoute
import com.app.builder.ui.component.navigation.NavigationState
import com.app.builder.ui.component.navigation.NavigationStore
import com.app.builder.ui.navigation.LocalRouter
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.navigation.scene.SplitSceneStrategy.Companion.split
import com.app.builder.ui.screen.equipmentdetail.EquipmentDetailScreen
import com.app.builder.ui.screen.equipmentdetail.EquipmentDetailScreenAction
import com.app.builder.ui.screen.equipmentdetail.EquipmentDetailScreenState
import com.app.builder.ui.screen.equipmentdetail.EquipmentDetailScreenStore
import com.app.builder.ui.screen.equipmentlist.EquipmentListScreen
import com.app.builder.ui.screen.equipmentlist.EquipmentListScreenAction
import com.app.builder.ui.screen.equipmentlist.EquipmentListScreenState
import com.app.builder.ui.screen.equipmentlist.EquipmentListScreenStore

/**
 * The equipment routes.
 *
 * @param useCases The use cases.
 */
fun EntryProviderScope<NavKey>.equipmentProvider(useCases: UseCases) {
    val properties = Equipment.Property.entries.map { it.name }.toPersistentList()
    val propertyMap = Equipment.Property.entries.associate { entry -> entry.name to entry.translationKey }.toImmutableMap()
    val defaultFilterCriteria = AppFile.ActionBarData(
        mode = ActionBarMode.DEFAULT.name,
        search = "",
        sortProperty = Equipment.Property.MODIFIED_AT.name,
        sortAscending = false,
        visibleProperties = properties,
        searchableProperties = properties,
    )
    val actionBarState = ActionBarState(
        sortProperty = defaultFilterCriteria.sortProperty,
        sortAscending = defaultFilterCriteria.sortAscending,
        properties = propertyMap,
        visibleProperties = properties,
        searchableProperties = properties
    )

    entry<Screen.EquipmentList>(metadata = split()) {
        val router = LocalRouter.current
        val store = viewModel { EquipmentListScreenStore(router = router, state = EquipmentListScreenState(), equipmentUseCases = useCases.equipmentUseCases) }
        EquipmentListScreen(
            actionBarStore = viewModel {
                ActionBarStore(
                    state = actionBarState.copy(title = "equipments", layout = ActionBarLayout.LIST),
                    router = router,
                    authenticationUseCases = useCases.authenticationUseCases,
                    storageFile = AppFile.EquipmentPreferences,
                    defaults = defaultFilterCriteria,
                    entityType = EntityType.EQUIPMENT,
                    onOkClick = { mode -> store.send(action = EquipmentListScreenAction.Ok(mode = mode)) }
                )
            },
            navigationStore = viewModel { NavigationStore(state = NavigationState(selected = NavigationRoute.EQUIPMENT), router = router, authenticationUseCases = useCases.authenticationUseCases) },
            store = store
        )
    }
    entry<Screen.EquipmentDetail>(metadata = split()) {
        val router = LocalRouter.current
        val splitScreen = LocalSplitScreen.current
        val store = viewModel { EquipmentDetailScreenStore(state = EquipmentDetailScreenState(), router = router, equipmentUseCases = useCases.equipmentUseCases, equipmentUuid = it.uuid) }
        EquipmentDetailScreen(
            actionBarStore = viewModel {
                ActionBarStore(
                    state = actionBarState.copy(title = "equipment", layout = if (splitScreen) ActionBarLayout.ALL else ActionBarLayout.DETAIL),
                    router = router,
                    authenticationUseCases = useCases.authenticationUseCases,
                    storageFile = AppFile.EquipmentPreferences,
                    defaults = defaultFilterCriteria,
                    entityType = EntityType.EQUIPMENT,
                    onOkClick = { mode -> store.send(action = EquipmentDetailScreenAction.Ok(mode = mode)) },
                    onCancelClick = { mode -> store.send(action = EquipmentDetailScreenAction.Cancel(mode = mode)) }
                )
            },
            navigationStore = viewModel { NavigationStore(state = NavigationState(selected = NavigationRoute.EQUIPMENT), router = router, authenticationUseCases = useCases.authenticationUseCases) },
            store = store
        )
    }
}
