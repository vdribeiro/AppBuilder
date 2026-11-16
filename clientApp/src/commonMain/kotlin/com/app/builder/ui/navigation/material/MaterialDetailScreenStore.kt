package com.app.builder.ui.navigation.material

import kotlin.time.Instant
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.firstOrNull
import com.app.builder.core.locale.now
import com.app.builder.core.security.toUuid
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.storage.AppFile
import com.app.builder.domain.BuildingMaterial
import com.app.builder.domain.Material
import com.app.builder.domain.gateway.building.BuildingUseCases
import com.app.builder.domain.gateway.buildingmaterial.BuildingMaterialUseCases
import com.app.builder.domain.gateway.buildingmaterial.BuildingOfMaterial
import com.app.builder.domain.gateway.material.MaterialUseCases
import com.app.builder.ui.navigation.Router
import com.app.builder.ui.navigation.building.buildingFilterDefaults
import com.app.builder.ui.store.Store

/**
 * Store backing the Material Detail Screen, observing the material and its linked buildings while viewing it, and applying, saving and discarding edits while editing it.
 *
 * @param state Initial Material Detail Screen state.
 * @property router The router used for navigation.
 * @property materialUseCases Use cases used to observe and save the material.
 * @property buildingMaterialUseCases Use cases used to observe and save the building materials of the material.
 * @property buildingUseCases Use cases used to observe every building.
 * @property materialUuid UUID of the material being displayed.
 * @property new Whether the material is new and not stored yet, starting it in edit mode.
 */
class MaterialDetailScreenStore(
    state: MaterialDetailScreenState,
    private val router: Router,
    private val materialUseCases: MaterialUseCases,
    private val buildingMaterialUseCases: BuildingMaterialUseCases,
    private val buildingUseCases: BuildingUseCases,
    private val materialUuid: String,
    private val new: Boolean = false
): Store<MaterialDetailScreenState, MaterialDetailScreenAction>(initialState = state) {
    init {
        setup()
    }

    /**
     * Starts a [new] material in edit mode, then observes the material with [materialUuid] and its linked buildings with their quantities, ignoring their changes while they're being edited, every building, and the building list filters.
     *
     * @return The [Job] representing this execution.
     */
    private fun setup(): Job = launch(id = "setup") {
        Telemetry.info(tag = TAG, message = "Setup")

        val uuid = materialUuid.toUuid() ?: run {
            Telemetry.info(tag = TAG, message = "Invalid material uuid")
            return@launch
        }

        if (new) updateState { it.copy(material = Material(uuid = uuid), editing = true, new = true) }

        materialUseCases.observeMaterial(uuid = uuid).observe(id = "material") { material ->
            updateState { if (it.editing) it.copy(storedMaterial = material) else it.copy(material = material, storedMaterial = material) }
        }
        buildingMaterialUseCases.observeBuildingsOfMaterial(materialUuid = uuid).observe(id = "buildings") { buildings ->
            val stored = buildings.toImmutableList()
            updateState { it.copy(storedBuildings = stored, buildings = if (it.editing) it.buildings else stored.toBuildingLinks()) }
        }
        buildingUseCases.observeBuildings().observe(id = "allBuildings") { buildings ->
            updateState { it.copy(allBuildings = buildings.filter { building -> building.deletedAt == null }.toImmutableList()) }
        }
        AppFile.BuildingPreferences.cache().observe(id = "buildingFilters") { filters ->
            updateState { it.copy(buildingFilters = filters ?: buildingFilterDefaults) }
        }

        Telemetry.info(tag = TAG, message = "Setup complete")
    }

    override fun reducer(state: MaterialDetailScreenState, action: MaterialDetailScreenAction) {
        super.reducer(state = state, action = action)
        when (action) {
            is MaterialDetailScreenAction.ChangeName -> updateState { it.copy(material = it.material?.copy(name = action.name)) }
            is MaterialDetailScreenAction.ChangeDescription -> updateState { it.copy(material = it.material?.copy(description = action.description)) }
            is MaterialDetailScreenAction.ChangeBuildingQuantity -> updateState { current ->
                current.copy(buildings = current.buildings.map { if (it.building.uuid == action.buildingUuid) it.copy(quantity = action.quantity) else it }.toImmutableList())
            }

            is MaterialDetailScreenAction.RemoveBuilding -> updateState { current ->
                current.copy(buildings = current.buildings.filter { it.building.uuid != action.buildingUuid }.toImmutableList())
            }

            MaterialDetailScreenAction.OpenBuildingPicker -> updateState { current ->
                current.copy(buildingPickerOpen = true, pickedBuildings = current.buildings.map { it.building.uuid }.toPersistentSet())
            }

            MaterialDetailScreenAction.CloseBuildingPicker -> updateState { it.copy(buildingPickerOpen = false, pickedBuildings = persistentSetOf()) }
            is MaterialDetailScreenAction.ToggleBuildingPick -> updateState { current ->
                val picked = if (action.buildingUuid in current.pickedBuildings) current.pickedBuildings - action.buildingUuid else current.pickedBuildings + action.buildingUuid
                current.copy(pickedBuildings = picked.toPersistentSet())
            }

            MaterialDetailScreenAction.SaveBuildingPick -> updateState { current ->
                val links = current.buildings.associateBy { it.building.uuid }
                val buildings = current.allBuildings
                    .filter { it.uuid in current.pickedBuildings }
                    .map { links[it.uuid] ?: BuildingLink(building = it) }
                    .toImmutableList()
                current.copy(buildings = buildings, buildingPickerOpen = false, pickedBuildings = persistentSetOf())
            }

            MaterialDetailScreenAction.Edit -> updateState { it.copy(editing = true) }
            MaterialDetailScreenAction.Save -> save(state = state)
            MaterialDetailScreenAction.Cancel -> cancel(state = state)
            MaterialDetailScreenAction.Delete -> delete(state = state)
            MaterialDetailScreenAction.Restore -> restore(state = state)
        }
    }

    /**
     * Saves the edited material when it differs from the stored one, then saves its building materials, and stops editing it, unless a quantity is invalid.
     *
     * @param state Material Detail Screen state at the time the save was requested.
     * @return The [Job] representing this execution.
     */
    private fun save(state: MaterialDetailScreenState): Job = launch(id = "save") {
        if (state.buildings.any { it.quantityError }) return@launch
        val edited = state.material ?: return@launch
        val stored = state.storedMaterial
        val modifiedAt = now()
        val material = when {
            stored != null && edited.copy(modifiedAt = stored.modifiedAt) == stored -> stored
            else -> edited.copy(modifiedAt = modifiedAt).also { materialUseCases.upsertMaterial(material = it) }
        }
        saveBuildings(state = state, modifiedAt = modifiedAt)
        updateState { it.copy(material = material, editing = false, new = false) }
    }

    /**
     * Upserts a building material for each building in [MaterialDetailScreenState.buildings] that is not in [MaterialDetailScreenState.storedBuildings] or has another quantity there, and deletes it for each building in the latter and not in the former.
     *
     * @param state Material Detail Screen state at the time the save was requested.
     * @param modifiedAt The modification time of the building materials.
     */
    private suspend fun saveBuildings(state: MaterialDetailScreenState, modifiedAt: Instant) {
        val material = state.material ?: return
        val edited = state.buildings.associate { it.building.uuid to it.quantity.toIntOrNull() }
        val stored = state.storedBuildings.associate { it.building.uuid to it.quantity }
        edited.forEach { (buildingUuid, quantity) ->
            if (quantity != null && stored[buildingUuid] != quantity) buildingMaterialUseCases.upsertBuildingMaterial(
                buildingMaterial = BuildingMaterial(modifiedAt = modifiedAt, buildingUuid = buildingUuid, materialUuid = material.uuid, quantity = quantity)
            )
        }
        (stored - edited.keys).forEach { (buildingUuid, quantity) ->
            buildingMaterialUseCases.upsertBuildingMaterial(
                buildingMaterial = BuildingMaterial(modifiedAt = modifiedAt, deletedAt = modifiedAt, buildingUuid = buildingUuid, materialUuid = material.uuid, quantity = quantity)
            )
        }
    }

    /**
     * Discards the edits, then reloads the stored material and its stored linked buildings and stops editing it, or leaves the screen when the material is new.
     *
     * @param state Material Detail Screen state at the time the cancel was requested.
     * @return The [Job] representing this execution.
     */
    private fun cancel(state: MaterialDetailScreenState): Job = launch(id = "cancel") {
        if (state.new) {
            router.back()
            return@launch
        }
        val material = state.material?.let { materialUseCases.observeMaterial(uuid = it.uuid).firstOrNull() }
        updateState { it.copy(material = material, editing = false, buildings = it.storedBuildings.toBuildingLinks(), buildingPickerOpen = false, pickedBuildings = persistentSetOf()) }
    }

    /**
     * Soft deletes the material by setting its deletion and modification times, then leaves the screen.
     *
     * @param state Material Detail Screen state at the time the delete was requested.
     * @return The [Job] representing this execution.
     */
    private fun delete(state: MaterialDetailScreenState): Job = launch(id = "delete") {
        val now = now()
        val material = state.material?.copy(deletedAt = now, modifiedAt = now)
        material?.let { materialUseCases.upsertMaterial(material = it) }
        router.back()
    }

    /**
     * Clears the deletion of the material, so it is active again, and keeps viewing it.
     *
     * @param state Material Detail Screen state at the time the restore was requested.
     * @return The [Job] representing this execution.
     */
    private fun restore(state: MaterialDetailScreenState): Job = launch(id = "restore") {
        val material = state.material?.copy(deletedAt = null, modifiedAt = now()) ?: return@launch
        materialUseCases.upsertMaterial(material = material)
        updateState { it.copy(material = material) }
    }

    /**
     * Converts these stored buildings of a material into [BuildingLink]s.
     *
     * @return The corresponding [BuildingLink]s, in the same order.
     */
    private fun List<BuildingOfMaterial>.toBuildingLinks(): ImmutableList<BuildingLink> = map { it.toBuildingLink() }.toImmutableList()

    companion object {
        private const val TAG = "MaterialDetailScreenStore"
    }
}
