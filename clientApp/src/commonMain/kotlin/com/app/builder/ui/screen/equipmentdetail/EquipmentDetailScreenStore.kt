package com.app.builder.ui.screen.equipmentdetail

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapNotNull
import com.app.builder.core.locale.now
import com.app.builder.core.security.toUuid
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.storage.AppFile
import com.app.builder.domain.Equipment
import com.app.builder.domain.gateway.equipment.EquipmentUseCases
import com.app.builder.toEnumOrNull
import com.app.builder.ui.component.bar.ActionBarMode
import com.app.builder.ui.navigation.Router
import com.app.builder.ui.store.Store

/**
 * Store backing the Equipment Detail Screen, observing the equipment and the action bar's edit mode, and applying name/code edits.
 *
 * @param state Initial Equipment Detail Screen state.
 * @property router The router used for navigation.
 * @property equipmentUseCases Use cases used to observe the equipment.
 * @property equipmentUuid UUID of the equipment being displayed.
 */
class EquipmentDetailScreenStore(
    state: EquipmentDetailScreenState,
    private val router: Router,
    private val equipmentUseCases: EquipmentUseCases,
    private val equipmentUuid: String
): Store<EquipmentDetailScreenState, EquipmentDetailScreenAction>(initialState = state) {
    init {
        setup()
    }

    /**
     * Observes the equipment with [equipmentUuid], creating a placeholder if it doesn't exist yet, and observes the cached action bar state to keep edit mode in sync.
     *
     * @return The [Job] representing this execution.
     */
    private fun setup(): Job = launch(id = "setup") {
        Telemetry.info(tag = TAG, message = "Setup")

        val uuid = equipmentUuid.toUuid() ?: run {
            Telemetry.info(tag = TAG, message = "Invalid equipment uuid")
            return@launch
        }

        equipmentUseCases.observeEquipment(uuid = uuid).observe(id = "equipment") { equipment ->
            val editMode = equipment == null
            val equipment = equipment ?: Equipment(
                uuid = uuid,
                modifiedAt = now(),
                deletedAt = null,
                name = "",
                code = ""
            )
            updateState {
                it.copy(
                    equipment = equipment,
                    editMode = editMode
                )
            }
        }

        AppFile.EquipmentPreferences.cache()
            .mapNotNull { it?.mode }
            .distinctUntilChanged()
            .observe(id = "syncMode") { newMode ->
                val mode = newMode.toEnumOrNull<ActionBarMode>() ?: return@observe
                when (mode) {
                    ActionBarMode.DEFAULT,
                    ActionBarMode.SEARCH,
                    ActionBarMode.DELETE,
                    ActionBarMode.BATCH_DELETE -> updateState { it.copy(editMode = false) }

                    ActionBarMode.ADD,
                    ActionBarMode.EDIT -> updateState { it.copy(editMode = true) }
                }
            }

        Telemetry.info(tag = TAG, message = "Setup complete")
    }

    override fun reducer(state: EquipmentDetailScreenState, action: EquipmentDetailScreenAction) {
        super.reducer(state = state, action = action)
        when (action) {
            is EquipmentDetailScreenAction.ChangeName -> updateState { it.copy(equipment = it.equipment?.copy(name = action.name)) }
            is EquipmentDetailScreenAction.ChangeCode -> updateState { it.copy(equipment = it.equipment?.copy(code = action.code)) }
            is EquipmentDetailScreenAction.Ok -> ok(state = state, action = action)
            is EquipmentDetailScreenAction.Cancel -> cancel(action = action)
        }
    }

    /**
     * Saves the edited equipment when the confirmed mode is one that edits it, and does nothing for the modes that do not.
     *
     * @param state Current equipment detail state, holding the equipment being edited.
     * @param action Action carrying the mode being confirmed.
     * @return The [Job] representing this execution.
     */
    private fun ok(state: EquipmentDetailScreenState, action: EquipmentDetailScreenAction.Ok): Job = launch(id = "ok") {
        when (action.mode) {
            ActionBarMode.DEFAULT,
            ActionBarMode.SEARCH,
            ActionBarMode.DELETE,
            ActionBarMode.BATCH_DELETE -> Unit

            ActionBarMode.ADD,
            ActionBarMode.EDIT -> state.equipment?.let { equipmentUseCases.upsertEquipment(equipment = it) }
        }
    }

    private fun cancel(action: EquipmentDetailScreenAction.Cancel): Job = launch(id = "cancel") {
        when (action.mode) {
            ActionBarMode.DEFAULT,
            ActionBarMode.SEARCH,
            ActionBarMode.EDIT,
            ActionBarMode.DELETE,
            ActionBarMode.BATCH_DELETE -> Unit

            ActionBarMode.ADD -> router.back()
        }

        updateState { it.copy(editMode = false) }
    }

    companion object {
        private const val TAG = "EquipmentDetailScreenStore"
    }
}
