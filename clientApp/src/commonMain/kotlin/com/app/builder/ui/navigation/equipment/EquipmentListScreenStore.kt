package com.app.builder.ui.navigation.equipment

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
import com.app.builder.domain.gateway.equipment.EquipmentUseCases
import com.app.builder.ui.navigation.Router
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.store.Store

/**
 * Store backing the equipment list, combining equipments with the shared action bar state to filter, sort, and select them.
 *
 * @param state Initial equipment list state.
 * @property router The router used for navigation.
 * @property equipmentUseCases Use cases used to observe and delete equipments.
 */
class EquipmentListScreenStore(
    state: EquipmentListScreenState,
    private val router: Router,
    private val equipmentUseCases: EquipmentUseCases
): Store<EquipmentListScreenState, EquipmentListScreenAction>(initialState = state) {
    init {
        setup()
    }

    override fun reducer(state: EquipmentListScreenState, action: EquipmentListScreenAction) {
        super.reducer(state = state, action = action)
        when (action) {
            is EquipmentListScreenAction.SelectEquipment -> selectEquipment(action = action)
        }
    }

    /**
     * Observes equipments and keeps the displayed list in sync.
     *
     * @return The [Job] representing this execution.
     */
    private fun setup(): Job = launch(id = "setup") {
        Telemetry.info(tag = TAG, message = "Setup")

        val equipmentsFlow = equipmentUseCases.observeEquipments().toStateFlow(initialValue = emptyList())
        val actionBarDataFlow = AppFile.EquipmentPreferences.cache()
            .mapNotNull { it }
            .distinctUntilChanged()
        val selectedUuidFlow = snapshotFlow { (router.backStack.lastOrNull { it is Screen.EquipmentDetail } as? Screen.EquipmentDetail)?.uuid }
            .distinctUntilChanged()

        val equipmentsActionBarFlow = combine(
            flow = equipmentsFlow,
            flow2 = actionBarDataFlow,
        ) { equipments, actionBarData ->
            equipments to actionBarData
        }.mapLatest { (equipments, actionBarData) ->
            equipments.filtered(actionBarData = actionBarData) to actionBarData
        }

        combine(
            flow = equipmentsActionBarFlow,
            flow2 = selectedUuidFlow
        ) { (equipments, actionBar), selectedUuid ->
            equipments
                .map { it.toEquipmentItem(visibilityProperties = actionBar.visibleProperties, selectedUuid = selectedUuid) }
                .toPersistentList()
        }
            .flowOn(context = Dispatcher.Default)
            .observe(id = "filterEquipments") { equipments ->
                updateState { it.copy(equipments = equipments) }
            }

        Telemetry.info(tag = TAG, message = "Setup complete")
    }

    /**
     * Navigates to the detail screen of the selected equipment.
     *
     * @param action Action carrying the UUID of the selected equipment.
     * @return The [Job] representing this execution.
     */
    private fun selectEquipment(action: EquipmentListScreenAction.SelectEquipment): Job = launch(id = "selectEquipment") {
        router.navigate(screen = Screen.EquipmentDetail(uuid = action.equipmentUuid), option = Router.NavOption.REPLACE_LAST)
    }

    companion object {
        private const val TAG = "EquipmentListScreenStore"
    }
}