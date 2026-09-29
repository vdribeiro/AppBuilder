package com.app.builder.ui.navigation.provider

import kotlinx.collections.immutable.toImmutableMap
import kotlinx.collections.immutable.toPersistentList
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.app.builder.data.storage.AppFile
import com.app.builder.domain.EntityType
import com.app.builder.domain.Building
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
import com.app.builder.ui.screen.buildingdetail.BuildingDetailScreen
import com.app.builder.ui.screen.buildingdetail.BuildingDetailScreenAction
import com.app.builder.ui.screen.buildingdetail.BuildingDetailScreenState
import com.app.builder.ui.screen.buildingdetail.BuildingDetailScreenStore
import com.app.builder.ui.screen.buildinglist.BuildingListScreen
import com.app.builder.ui.screen.buildinglist.BuildingListScreenAction
import com.app.builder.ui.screen.buildinglist.BuildingListScreenState
import com.app.builder.ui.screen.buildinglist.BuildingListScreenStore

/**
 * The building routes.
 *
 * @param useCases The use cases.
 */
fun EntryProviderScope<NavKey>.buildingProvider(useCases: UseCases) {
    val properties = Building.Property.entries.map { it.name }.toPersistentList()
    val propertyMap = Building.Property.entries.associate { entry -> entry.name to entry.translationKey }.toImmutableMap()
    val defaultFilterCriteria = AppFile.ActionBarData(
        mode = ActionBarMode.DEFAULT.name,
        search = "",
        sortProperty = Building.Property.MODIFIED_AT.name,
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

    entry<Screen.BuildingList>(metadata = split()) {
        val router = LocalRouter.current
        val store = viewModel { BuildingListScreenStore(router = router, state = BuildingListScreenState(), buildingUseCases = useCases.buildingUseCases) }
        BuildingListScreen(
            actionBarStore = viewModel {
                ActionBarStore(
                    state = actionBarState.copy(title = "buildings", layout = ActionBarLayout.LIST),
                    router = router,
                    authenticationUseCases = useCases.authenticationUseCases,
                    storageFile = AppFile.BuildingPreferences,
                    defaults = defaultFilterCriteria,
                    entityType = EntityType.BUILDING,
                    onOkClick = { mode -> store.send(action = BuildingListScreenAction.Ok(mode = mode)) }
                )
            },
            navigationStore = viewModel { NavigationStore(state = NavigationState(selected = NavigationRoute.BUILDING), router = router, authenticationUseCases = useCases.authenticationUseCases) },
            store = store
        )
    }
    entry<Screen.BuildingDetail>(metadata = split()) {
        val router = LocalRouter.current
        val splitScreen = LocalSplitScreen.current
        val store = viewModel { BuildingDetailScreenStore(state = BuildingDetailScreenState(), router = router, buildingUseCases = useCases.buildingUseCases, buildingUuid = it.uuid) }
        BuildingDetailScreen(
            actionBarStore = viewModel {
                ActionBarStore(
                    state = actionBarState.copy(title = "building", layout = if (splitScreen) ActionBarLayout.ALL else ActionBarLayout.DETAIL),
                    router = router,
                    authenticationUseCases = useCases.authenticationUseCases,
                    storageFile = AppFile.BuildingPreferences,
                    defaults = defaultFilterCriteria,
                    entityType = EntityType.BUILDING,
                    onOkClick = { mode -> store.send(action = BuildingDetailScreenAction.Ok(mode = mode)) },
                    onCancelClick = { mode -> store.send(action = BuildingDetailScreenAction.Cancel(mode = mode)) }
                )
            },
            navigationStore = viewModel { NavigationStore(state = NavigationState(selected = NavigationRoute.BUILDING), router = router, authenticationUseCases = useCases.authenticationUseCases) },
            store = store
        )
    }
}
