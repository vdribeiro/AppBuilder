package com.app.builder.ui.navigation.equipment

import kotlin.time.Instant
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.firstOrNull
import com.app.builder.core.locale.now
import com.app.builder.core.security.toUuid
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.storage.AppFile
import com.app.builder.domain.BuildingEquipment
import com.app.builder.domain.Equipment
import com.app.builder.domain.gateway.building.BuildingUseCases
import com.app.builder.domain.gateway.buildingequipment.BuildingEquipmentUseCases
import com.app.builder.domain.gateway.equipment.EquipmentUseCases
import com.app.builder.ui.navigation.Router
import com.app.builder.ui.navigation.building.buildingFilterDefaults
import com.app.builder.ui.store.Store

/**
 * Store backing the Equipment Detail Screen, observing the equipment and its linked buildings while viewing it, and applying, saving and discarding edits while editing it.
 *
 * @param state Initial Equipment Detail Screen state.
 * @property router The router used for navigation.
 * @property equipmentUseCases Use cases used to observe and save the equipment.
 * @property buildingEquipmentUseCases Use cases used to observe and save the building equipments of the equipment.
 * @property buildingUseCases Use cases used to observe every building.
 * @property equipmentUuid UUID of the equipment being displayed.
 * @property new Whether the equipment is new and not stored yet, starting it in edit mode.
 */
class EquipmentDetailScreenStore(
    state: EquipmentDetailScreenState,
    private val router: Router,
    private val equipmentUseCases: EquipmentUseCases,
    private val buildingEquipmentUseCases: BuildingEquipmentUseCases,
    private val buildingUseCases: BuildingUseCases,
    private val equipmentUuid: String,
    private val new: Boolean = false
): Store<EquipmentDetailScreenState, EquipmentDetailScreenAction>(initialState = state) {
    init {
        setup()
    }

    /**
     * Starts a [new] equipment in edit mode, then observes the equipment with [equipmentUuid] and its linked buildings, ignoring their changes while they're being edited, every building, and the building list filters.
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
            updateState { if (it.editing) it.copy(storedEquipment = equipment) else it.copy(equipment = equipment, storedEquipment = equipment) }
        }
        buildingEquipmentUseCases.observeBuildingsOfEquipment(equipmentUuid = uuid).observe(id = "buildings") { buildings ->
            val stored = buildings.toImmutableList()
            updateState { it.copy(storedBuildings = stored, buildings = if (it.editing) it.buildings else stored) }
        }
        buildingUseCases.observeBuildings().observe(id = "allBuildings") { buildings ->
            updateState { it.copy(allBuildings = buildings.filter { building -> building.deletedAt == null }.toImmutableList()) }
        }
        AppFile.BuildingPreferences.cache().observe(id = "buildingFilters") { filters ->
            updateState { it.copy(buildingFilters = filters ?: buildingFilterDefaults) }
        }

        Telemetry.info(tag = TAG, message = "Setup complete")
    }

    override fun reducer(state: EquipmentDetailScreenState, action: EquipmentDetailScreenAction) {
        super.reducer(state = state, action = action)
        when (action) {
            is EquipmentDetailScreenAction.ChangeName -> updateState { it.copy(equipment = it.equipment?.copy(name = action.name)) }
            is EquipmentDetailScreenAction.ChangeDescription -> updateState { it.copy(equipment = it.equipment?.copy(description = action.description)) }
            is EquipmentDetailScreenAction.ChangeStatus -> updateState { it.copy(equipment = it.equipment?.copy(status = action.status)) }
            is EquipmentDetailScreenAction.RemoveBuilding -> updateState { current ->
                current.copy(buildings = current.buildings.filter { it.uuid != action.buildingUuid }.toImmutableList())
            }

            EquipmentDetailScreenAction.OpenBuildingPicker -> updateState { current ->
                current.copy(buildingPickerOpen = true, pickedBuildings = current.buildings.map { it.uuid }.toPersistentSet())
            }

            EquipmentDetailScreenAction.CloseBuildingPicker -> updateState { it.copy(buildingPickerOpen = false, pickedBuildings = persistentSetOf()) }
            is EquipmentDetailScreenAction.ToggleBuildingPick -> updateState { current ->
                val picked = if (action.buildingUuid in current.pickedBuildings) current.pickedBuildings - action.buildingUuid else current.pickedBuildings + action.buildingUuid
                current.copy(pickedBuildings = picked.toPersistentSet())
            }

            EquipmentDetailScreenAction.SaveBuildingPick -> updateState { current ->
                val buildings = current.allBuildings.filter { it.uuid in current.pickedBuildings }.toImmutableList()
                current.copy(buildings = buildings, buildingPickerOpen = false, pickedBuildings = persistentSetOf())
            }

            EquipmentDetailScreenAction.Edit -> updateState { it.copy(editing = true) }
            EquipmentDetailScreenAction.Save -> save(state = state)
            EquipmentDetailScreenAction.Cancel -> cancel(state = state)
            EquipmentDetailScreenAction.Delete -> delete(state = state)
            EquipmentDetailScreenAction.Restore -> restore(state = state)
        }
    }

    /**
     * Saves the edited equipment when it differs from the stored one, then saves its building equipments, and stops editing it.
     *
     * @param state Equipment Detail Screen state at the time the save was requested.
     * @return The [Job] representing this execution.
     */
    private fun save(state: EquipmentDetailScreenState): Job = launch(id = "save") {
        val edited = state.equipment ?: return@launch
        val stored = state.storedEquipment
        val modifiedAt = now()
        val equipment = when {
            stored != null && edited.copy(modifiedAt = stored.modifiedAt) == stored -> stored
            else -> edited.copy(modifiedAt = modifiedAt).also { equipmentUseCases.upsertEquipment(equipment = it) }
        }
        saveBuildings(state = state, modifiedAt = modifiedAt)
        updateState { it.copy(equipment = equipment, editing = false, new = false) }
    }

    /**
     * Upserts a building equipment for each building in [EquipmentDetailScreenState.buildings] and not in [EquipmentDetailScreenState.storedBuildings], and deletes it for each building in the latter and not in the former.
     *
     * @param state Equipment Detail Screen state at the time the save was requested.
     * @param modifiedAt The modification time of the building equipments.
     */
    private suspend fun saveBuildings(state: EquipmentDetailScreenState, modifiedAt: Instant) {
        val equipment = state.equipment ?: return
        val edited = state.buildings.map { it.uuid }.toSet()
        val stored = state.storedBuildings.map { it.uuid }.toSet()
        (edited - stored).forEach { buildingUuid ->
            buildingEquipmentUseCases.upsertBuildingEquipment(
                buildingEquipment = BuildingEquipment(modifiedAt = modifiedAt, buildingUuid = buildingUuid, equipmentUuid = equipment.uuid)
            )
        }
        (stored - edited).forEach { buildingUuid ->
            buildingEquipmentUseCases.upsertBuildingEquipment(
                buildingEquipment = BuildingEquipment(modifiedAt = modifiedAt, deletedAt = modifiedAt, buildingUuid = buildingUuid, equipmentUuid = equipment.uuid)
            )
        }
    }

    /**
     * Discards the edits, then reloads the stored equipment and its stored linked buildings and stops editing it, or leaves the screen when the equipment is new.
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
        updateState { it.copy(equipment = equipment, editing = false, buildings = it.storedBuildings, buildingPickerOpen = false, pickedBuildings = persistentSetOf()) }
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
