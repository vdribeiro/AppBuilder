package com.app.builder.ui.navigation.equipment

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.firstOrNull
import com.app.builder.core.locale.now
import com.app.builder.core.security.toUuid
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.domain.Equipment
import com.app.builder.domain.gateway.equipment.EquipmentUseCases
import com.app.builder.ui.navigation.Router
import com.app.builder.ui.store.Store

/**
 * Store backing the Equipment Detail Screen, observing the equipment while viewing it, and applying, saving and discarding edits while editing it.
 *
 * @param state Initial Equipment Detail Screen state.
 * @property router The router used for navigation.
 * @property equipmentUseCases Use cases used to observe and save the equipment.
 * @property equipmentUuid UUID of the equipment being displayed.
 * @property new Whether the equipment is new and not stored yet, starting it in edit mode.
 */
class EquipmentDetailScreenStore(
    state: EquipmentDetailScreenState,
    private val router: Router,
    private val equipmentUseCases: EquipmentUseCases,
    private val equipmentUuid: String,
    private val new: Boolean = false
): Store<EquipmentDetailScreenState, EquipmentDetailScreenAction>(initialState = state) {
    init {
        setup()
    }

    /**
     * Starts a [new] equipment in edit mode, then observes the equipment with [equipmentUuid], ignoring its changes while it's being edited.
     *
     * @return The [Job] representing this execution.
     */
    private fun setup(): Job = launch(id = "setup") {
        Telemetry.info(tag = TAG, message = "Setup")

        val uuid = equipmentUuid.toUuid() ?: run {
            Telemetry.info(tag = TAG, message = "Invalid equipment uuid")
            return@launch
        }

        if (new) updateState { it.copy(equipment = Equipment(uuid = uuid), editing = true, new = true) }

        equipmentUseCases.observeEquipment(uuid = uuid).observe(id = "equipment") { equipment ->
            updateState { if (it.editing) it else it.copy(equipment = equipment) }
        }

        Telemetry.info(tag = TAG, message = "Setup complete")
    }

    override fun reducer(state: EquipmentDetailScreenState, action: EquipmentDetailScreenAction) {
        super.reducer(state = state, action = action)
        when (action) {
            is EquipmentDetailScreenAction.ChangeName -> updateState { it.copy(equipment = it.equipment?.copy(name = action.name)) }
            is EquipmentDetailScreenAction.ChangeDescription -> updateState { it.copy(equipment = it.equipment?.copy(description = action.description)) }
            is EquipmentDetailScreenAction.ChangeStatus -> updateState { it.copy(equipment = it.equipment?.copy(status = action.status)) }
            EquipmentDetailScreenAction.Edit -> updateState { it.copy(editing = true) }
            EquipmentDetailScreenAction.Save -> save(state = state)
            EquipmentDetailScreenAction.Cancel -> cancel(state = state)
            EquipmentDetailScreenAction.Delete -> delete(state = state)
            EquipmentDetailScreenAction.Restore -> restore(state = state)
        }
    }

    /**
     * Saves the edited equipment and stops editing it.
     *
     * @param state Equipment Detail Screen state at the time the save was requested.
     * @return The [Job] representing this execution.
     */
    private fun save(state: EquipmentDetailScreenState): Job = launch(id = "save") {
        val equipment = state.equipment?.copy(modifiedAt = now()) ?: return@launch
        equipmentUseCases.upsertEquipment(equipment = equipment)
        updateState { it.copy(equipment = equipment, editing = false, new = false) }
    }

    /**
     * Discards the edits, then reloads the stored equipment and stops editing it, or leaves the screen when the equipment is new.
     *
     * @param state Equipment Detail Screen state at the time the cancel was requested.
     * @return The [Job] representing this execution.
     */
    private fun cancel(state: EquipmentDetailScreenState): Job = launch(id = "cancel") {
        if (state.new) {
            router.back()
            return@launch
        }
        val equipment = state.equipment?.let { equipmentUseCases.observeEquipment(uuid = it.uuid).firstOrNull() }
        updateState { it.copy(equipment = equipment, editing = false) }
    }

    /**
     * Soft deletes the equipment by setting its deletion and modification times, then leaves the screen.
     *
     * @param state Equipment Detail Screen state at the time the delete was requested.
     * @return The [Job] representing this execution.
     */
    private fun delete(state: EquipmentDetailScreenState): Job = launch(id = "delete") {
        val now = now()
        val equipment = state.equipment?.copy(deletedAt = now, modifiedAt = now)
        equipment?.let { equipmentUseCases.upsertEquipment(equipment = it) }
        router.back()
    }

    /**
     * Clears the deletion of the equipment, so it is active again, and keeps viewing it.
     *
     * @param state Equipment Detail Screen state at the time the restore was requested.
     * @return The [Job] representing this execution.
     */
    private fun restore(state: EquipmentDetailScreenState): Job = launch(id = "restore") {
        val equipment = state.equipment?.copy(deletedAt = null, modifiedAt = now()) ?: return@launch
        equipmentUseCases.upsertEquipment(equipment = equipment)
        updateState { it.copy(equipment = equipment) }
    }

    companion object {
        private const val TAG = "EquipmentDetailScreenStore"
    }
}
