package com.app.builder.ui.navigation.material

import kotlin.time.Instant
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import com.app.builder.core.config.ClientFlags
import com.app.builder.core.locale.now
import com.app.builder.core.security.toUuid
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.domain.BuildingMaterial
import com.app.builder.domain.EntityType
import com.app.builder.domain.Material
import com.app.builder.domain.Permission
import com.app.builder.domain.gateway.authentication.AuthenticationUseCases
import com.app.builder.domain.gateway.buildingmaterial.BuildingMaterialUseCases
import com.app.builder.domain.gateway.material.MaterialUseCases
import com.app.builder.ui.component.link.Link
import com.app.builder.ui.component.link.linkChanges
import com.app.builder.ui.component.link.linkPermissions
import com.app.builder.ui.navigation.Router
import com.app.builder.ui.store.Store

/**
 * Store backing the Material Detail Screen, observing the material and its linked buildings while viewing it, and applying, saving and discarding edits while editing it.
 *
 * @param state Initial Material Detail Screen state.
 * @property router The router used for navigation.
 * @property materialUseCases Use cases used to observe and save the material.
 * @property buildingMaterialUseCases Use cases used to observe and save the building materials of the material.
 * @property authenticationUseCases Use cases used to observe the current account.
 * @property materialUuid UUID of the material being displayed.
 * @property new Whether the material is new and not stored yet, starting it in edit mode.
 */
class MaterialDetailScreenStore(
    state: MaterialDetailScreenState,
    private val router: Router,
    private val materialUseCases: MaterialUseCases,
    private val buildingMaterialUseCases: BuildingMaterialUseCases,
    private val authenticationUseCases: AuthenticationUseCases,
    private val materialUuid: String,
    private val new: Boolean = false
): Store<MaterialDetailScreenState, MaterialDetailScreenAction>(initialState = state) {
    init {
        setup()
    }

    /**
     * Starts a [new] material in edit mode, then observes what the current account can do with the material and its links, and the material with [materialUuid] and its linked buildings with their quantities, ignoring their changes while they're being edited.
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

        combine(
            flow = authenticationUseCases.observeCurrentAccount(),
            flow2 = ClientFlags.flow
        ) { account, flags -> account to flags }.observe(id = "permissions") { (account, flags) ->
            val writable = account?.hasPermission(entityType = EntityType.MATERIAL, permission = Permission.WRITE) ?: false
            val buildingLinks = account.linkPermissions(enabled = flags.buildingMaterials, linked = EntityType.BUILDING, link = EntityType.BUILDING_MATERIAL)
            updateState { it.copy(writable = writable, buildingLinks = buildingLinks) }
        }
        materialUseCases.observeMaterial(uuid = uuid).observe(id = "material") { material ->
            updateState { if (it.editing) it.copy(storedMaterial = material) else it.copy(material = material, storedMaterial = material) }
        }
        buildingMaterialUseCases.observeBuildingsOfMaterial(materialUuid = uuid).observe(id = "buildings") { buildings ->
            val stored = buildings.map { Link(uuid = it.building.uuid, name = it.building.name, quantity = it.quantity.toString()) }.toImmutableList()
            updateState { it.copy(storedBuildings = stored, buildings = if (it.editing) it.buildings else stored) }
        }

        Telemetry.info(tag = TAG, message = "Setup complete")
    }

    override fun reducer(state: MaterialDetailScreenState, action: MaterialDetailScreenAction) {
        super.reducer(state = state, action = action)
        when (action) {
            is MaterialDetailScreenAction.ChangeName -> updateState { it.copy(material = it.material?.copy(name = action.name)) }
            is MaterialDetailScreenAction.ChangeDescription -> updateState { it.copy(material = it.material?.copy(description = action.description)) }
            is MaterialDetailScreenAction.ChangeBuildingQuantity -> updateState { current ->
                current.copy(buildings = current.buildings.map { if (it.uuid == action.buildingUuid) it.copy(quantity = action.quantity) else it }.toImmutableList())
            }

            is MaterialDetailScreenAction.RemoveBuilding -> updateState { current ->
                current.copy(buildings = current.buildings.filter { it.uuid != action.buildingUuid }.toImmutableList())
            }

            is MaterialDetailScreenAction.SetBuildings -> updateState { current ->
                val links = current.buildings.associateBy { it.uuid }
                current.copy(buildings = action.buildings.map { links[it.uuid] ?: it.copy(quantity = "1") }.toImmutableList())
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
        val changes = linkChanges(edited = state.buildings, stored = state.storedBuildings)
        changes.upserted.forEach { link ->
            val quantity = link.quantity?.toIntOrNull() ?: return@forEach
            buildingMaterialUseCases.upsertBuildingMaterial(
                buildingMaterial = BuildingMaterial(modifiedAt = modifiedAt, buildingUuid = link.uuid, materialUuid = material.uuid, quantity = quantity)
            )
        }
        changes.removed.forEach { link ->
            val quantity = link.quantity?.toIntOrNull() ?: return@forEach
            buildingMaterialUseCases.upsertBuildingMaterial(
                buildingMaterial = BuildingMaterial(modifiedAt = modifiedAt, deletedAt = modifiedAt, buildingUuid = link.uuid, materialUuid = material.uuid, quantity = quantity)
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
        updateState { it.copy(material = material, editing = false, buildings = it.storedBuildings) }
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

    companion object {
        private const val TAG = "MaterialDetailScreenStore"
    }
}
