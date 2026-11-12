package com.app.builder.ui.navigation.building

import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.mapNotNull
import androidx.compose.runtime.snapshotFlow
import com.app.builder.core.flow.Dispatcher
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.storage.AppFile
import com.app.builder.domain.gateway.building.BuildingUseCases
import com.app.builder.ui.navigation.Router
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.store.Store

/**
 * Store backing the building list, combining buildings with the shared action bar state to filter, sort, and select them.
 *
 * @param state Initial building list state.
 * @property router The router used for navigation.
 * @property buildingUseCases Use cases used to observe and delete buildings.
 */
class BuildingListScreenStore(
    state: BuildingListScreenState,
    private val router: Router,
    private val buildingUseCases: BuildingUseCases
): Store<BuildingListScreenState, BuildingListScreenAction>(initialState = state) {
    init {
        setup()
    }

    override fun reducer(state: BuildingListScreenState, action: BuildingListScreenAction) {
        super.reducer(state = state, action = action)
        when (action) {
            is BuildingListScreenAction.SelectBuilding -> selectBuilding(action = action)
        }
    }

    /**
     * Observes buildings and keeps the displayed list in sync.
     *
     * @return The [Job] representing this execution.
     */
    private fun setup(): Job = launch(id = "setup") {
        Telemetry.info(tag = TAG, message = "Setup")

        val buildingsFlow = buildingUseCases.observeBuildings().toStateFlow(initialValue = emptyList())
        val actionBarDataFlow = AppFile.BuildingPreferences.cache()
            .mapNotNull { it }
            .distinctUntilChanged()
        val selectedUuidFlow = snapshotFlow { (router.backStack.lastOrNull { it is Screen.BuildingDetail } as? Screen.BuildingDetail)?.uuid }
            .distinctUntilChanged()

        val buildingsActionBarFlow = combine(
            flow = buildingsFlow,
            flow2 = actionBarDataFlow,
        ) { buildings, actionBarData ->
            buildings to actionBarData
        }.mapLatest { (buildings, actionBarData) ->
            buildings.filtered(actionBarData = actionBarData) to actionBarData
        }

        combine(
            flow = buildingsActionBarFlow,
            flow2 = selectedUuidFlow
        ) { (buildings, actionBar), selectedUuid ->
            buildings
                .map { it.toBuildingItem(visibilityProperties = actionBar.visibleProperties, selectedUuid = selectedUuid) }
                .toPersistentList()
        }
            .flowOn(context = Dispatcher.Default)
            .observe(id = "filterBuildings") { buildings ->
                updateState { it.copy(buildings = buildings) }
            }

        Telemetry.info(tag = TAG, message = "Setup complete")
    }

    /**
     * Navigates to the detail screen of the selected building.
     *
     * @param action Action carrying the UUID of the selected building.
     * @return The [Job] representing this execution.
     */
    private fun selectBuilding(action: BuildingListScreenAction.SelectBuilding): Job = launch(id = "selectBuilding") {
        router.navigate(screen = Screen.BuildingDetail(uuid = action.buildingUuid), option = Router.NavOption.REPLACE_LAST)
    }

    companion object {
        private const val TAG = "BuildingListScreenStore"
    }
}