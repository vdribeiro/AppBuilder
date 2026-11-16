package com.app.builder.ui.navigation.building

import kotlin.math.round
import kotlin.time.Instant
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import com.app.builder.core.config.ClientFlags
import com.app.builder.core.locale.now
import com.app.builder.core.security.toUuid
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.domain.Building
import com.app.builder.domain.BuildingEquipment
import com.app.builder.domain.BuildingMaterial
import com.app.builder.domain.EntityType
import com.app.builder.domain.Permission
import com.app.builder.domain.gateway.authentication.AuthenticationUseCases
import com.app.builder.domain.gateway.building.BuildingUseCases
import com.app.builder.domain.gateway.buildingequipment.BuildingEquipmentUseCases
import com.app.builder.domain.gateway.buildingmaterial.BuildingMaterialUseCases
import com.app.builder.ui.component.link.Link
import com.app.builder.ui.component.link.linkChanges
import com.app.builder.ui.component.link.linkPermissions
import com.app.builder.ui.navigation.Router
import com.app.builder.ui.store.Store

/**
 * Store backing the Building Detail Screen, observing the building and its linked equipments and materials while viewing it, and applying, saving and discarding edits while editing it.
 *
 * @param state Initial Building Detail Screen state.
 * @property router The router used for navigation.
 * @property buildingUseCases Use cases used to observe and save the building.
 * @property buildingEquipmentUseCases Use cases used to observe and save the building equipments of the building.
 * @property buildingMaterialUseCases Use cases used to observe and save the building materials of the building.
 * @property authenticationUseCases Use cases used to observe the current account.
 * @property buildingUuid UUID of the building being displayed.
 * @property new Whether the building is new and not stored yet, starting it in edit mode.
 */
class BuildingDetailScreenStore(
    state: BuildingDetailScreenState,
    private val router: Router,
    private val buildingUseCases: BuildingUseCases,
    private val buildingEquipmentUseCases: BuildingEquipmentUseCases,
    private val buildingMaterialUseCases: BuildingMaterialUseCases,
    private val authenticationUseCases: AuthenticationUseCases,
    private val buildingUuid: String,
    private val new: Boolean = false
): Store<BuildingDetailScreenState, BuildingDetailScreenAction>(initialState = state) {
    init {
        setup()
    }

    /**
     * Starts a [new] building in edit mode, then observes what the current account can do with the building and its links, and the building with [buildingUuid] and its linked equipments and materials, ignoring their changes while they're being edited.
     *
     * @return The [Job] representing this execution.
     */
    private fun setup(): Job = launch(id = "setup") {
        Telemetry.info(tag = TAG, message = "Setup")

        val uuid = buildingUuid.toUuid() ?: run {
            Telemetry.info(tag = TAG, message = "Invalid building uuid")
            return@launch
        }

        if (new) updateState { it.showing(building = Building(uuid = uuid)).copy(editing = true, new = true) }

        combine(
            flow = authenticationUseCases.observeCurrentAccount(),
            flow2 = ClientFlags.flow
        ) { account, flags -> account to flags }.observe(id = "current_account") { (account, flags) ->
            val writable = account?.hasPermission(entityType = EntityType.BUILDING, permission = Permission.WRITE) ?: false
            val equipmentLinks = account.linkPermissions(enabled = flags.buildingEquipments, linked = EntityType.EQUIPMENT, link = EntityType.BUILDING_EQUIPMENT)
            val materialLinks = account.linkPermissions(enabled = flags.buildingMaterials, linked = EntityType.MATERIAL, link = EntityType.BUILDING_MATERIAL)
            updateState { it.copy(writable = writable, equipmentLinks = equipmentLinks, materialLinks = materialLinks) }
        }
        buildingUseCases.observeBuilding(uuid = uuid).observe(id = "building") { building ->
            updateState { if (it.editing) it.copy(storedBuilding = building) else it.showing(building = building).copy(storedBuilding = building) }
        }
        buildingEquipmentUseCases.observeEquipmentsInBuilding(buildingUuid = uuid).observe(id = "equipments") { equipments ->
            val stored = equipments.map { Link(uuid = it.uuid, name = it.name) }.toImmutableList()
            updateState { it.copy(storedEquipments = stored, equipments = if (it.editing) it.equipments else stored) }
        }
        buildingMaterialUseCases.observeMaterialsInBuilding(buildingUuid = uuid).observe(id = "materials") { materials ->
            val stored = materials.map { Link(uuid = it.material.uuid, name = it.material.name, quantity = it.quantity.toString()) }.toImmutableList()
            updateState { it.copy(storedMaterials = stored, materials = if (it.editing) it.materials else stored) }
        }

        Telemetry.info(tag = TAG, message = "Setup complete")
    }

    override fun reducer(state: BuildingDetailScreenState, action: BuildingDetailScreenAction) {
        super.reducer(state = state, action = action)
        when (action) {
            is BuildingDetailScreenAction.ChangeName -> updateState { it.copy(building = it.building?.copy(name = action.name)) }
            is BuildingDetailScreenAction.ChangeDescription -> updateState { it.copy(building = it.building?.copy(description = action.description)) }
            is BuildingDetailScreenAction.ChangeAddress -> updateState { it.copy(building = it.building?.copy(address = action.address)) }
            is BuildingDetailScreenAction.ChangeLatitude -> updateState { it.copy(latitude = action.latitude) }
            is BuildingDetailScreenAction.ChangeLongitude -> updateState { it.copy(longitude = action.longitude) }
            is BuildingDetailScreenAction.ChangeCoordinates -> updateState { it.copy(latitude = action.latitude.rounded().toString(), longitude = action.longitude.rounded().toString()) }
            is BuildingDetailScreenAction.SetEquipments -> updateState { it.copy(equipments = action.equipments) }
            is BuildingDetailScreenAction.RemoveEquipment -> updateState { current ->
                current.copy(equipments = current.equipments.filter { it.uuid != action.equipmentUuid }.toImmutableList())
            }

            is BuildingDetailScreenAction.SetMaterials -> updateState { current ->
                val links = current.materials.associateBy { it.uuid }
                current.copy(materials = action.materials.map { links[it.uuid] ?: it.copy(quantity = "1") }.toImmutableList())
            }

            is BuildingDetailScreenAction.RemoveMaterial -> updateState { current ->
                current.copy(materials = current.materials.filter { it.uuid != action.materialUuid }.toImmutableList())
            }

            is BuildingDetailScreenAction.ChangeMaterialQuantity -> updateState { current ->
                current.copy(materials = current.materials.map { if (it.uuid == action.materialUuid) it.copy(quantity = action.quantity) else it }.toImmutableList())
            }

            BuildingDetailScreenAction.Edit -> updateState { it.copy(editing = true) }
            BuildingDetailScreenAction.Save -> save(state = state)
            BuildingDetailScreenAction.Cancel -> cancel(state = state)
            BuildingDetailScreenAction.Delete -> delete(state = state)
            BuildingDetailScreenAction.Restore -> restore(state = state)
        }
    }

    /**
     * Saves the edited building with the typed coordinates when it differs from the stored one, then saves its building equipments and building materials, and stops editing it, unless a coordinate or a quantity is invalid.
     *
     * @param state Building Detail Screen state at the time the save was requested.
     * @return The [Job] representing this execution.
     */
    private fun save(state: BuildingDetailScreenState): Job = launch(id = "save") {
        if (state.latitudeError || state.longitudeError || state.materials.any { it.quantityError }) return@launch
        val edited = state.building?.copy(
            latitude = state.latitude.toDoubleOrNull(),
            longitude = state.longitude.toDoubleOrNull()
        ) ?: return@launch
        val stored = state.storedBuilding
        val modifiedAt = now()
        val building = when {
            stored != null && edited.copy(modifiedAt = stored.modifiedAt) == stored -> stored
            else -> edited.copy(modifiedAt = modifiedAt).also { buildingUseCases.upsertBuilding(building = it) }
        }
        saveEquipments(state = state, modifiedAt = modifiedAt)
        saveMaterials(state = state, modifiedAt = modifiedAt)
        updateState { it.showing(building = building).copy(editing = false, new = false) }
    }

    /**
     * Upserts a building equipment for each equipment in [BuildingDetailScreenState.equipments] and not in [BuildingDetailScreenState.storedEquipments], and deletes it for each equipment in the latter and not in the former.
     *
     * @param state Building Detail Screen state at the time the save was requested.
     * @param modifiedAt The modification time of the building equipments.
     */
    private suspend fun saveEquipments(state: BuildingDetailScreenState, modifiedAt: Instant) {
        val building = state.building ?: return
        val changes = linkChanges(edited = state.equipments, stored = state.storedEquipments)
        changes.upserted.forEach { link ->
            buildingEquipmentUseCases.upsertBuildingEquipment(
                buildingEquipment = BuildingEquipment(modifiedAt = modifiedAt, buildingUuid = building.uuid, equipmentUuid = link.uuid)
            )
        }
        changes.removed.forEach { link ->
            buildingEquipmentUseCases.upsertBuildingEquipment(
                buildingEquipment = BuildingEquipment(modifiedAt = modifiedAt, deletedAt = modifiedAt, buildingUuid = building.uuid, equipmentUuid = link.uuid)
            )
        }
    }

    /**
     * Upserts a building material for each material in [BuildingDetailScreenState.materials] that is not in [BuildingDetailScreenState.storedMaterials] or has another quantity there, and deletes it for each material in the latter and not in the former.
     *
     * @param state Building Detail Screen state at the time the save was requested.
     * @param modifiedAt The modification time of the building materials.
     */
    private suspend fun saveMaterials(state: BuildingDetailScreenState, modifiedAt: Instant) {
        val building = state.building ?: return
        val changes = linkChanges(edited = state.materials, stored = state.storedMaterials)
        changes.upserted.forEach { link ->
            val quantity = link.quantity?.toIntOrNull() ?: return@forEach
            buildingMaterialUseCases.upsertBuildingMaterial(
                buildingMaterial = BuildingMaterial(modifiedAt = modifiedAt, buildingUuid = building.uuid, materialUuid = link.uuid, quantity = quantity)
            )
        }
        changes.removed.forEach { link ->
            val quantity = link.quantity?.toIntOrNull() ?: return@forEach
            buildingMaterialUseCases.upsertBuildingMaterial(
                buildingMaterial = BuildingMaterial(modifiedAt = modifiedAt, deletedAt = modifiedAt, buildingUuid = building.uuid, materialUuid = link.uuid, quantity = quantity)
            )
        }
    }

    /**
     * Discards the edits, then reloads the stored building and its stored linked equipments and materials and stops editing it, or leaves the screen when the building is new.
     *
     * @param state Building Detail Screen state at the time the cancel was requested.
     * @return The [Job] representing this execution.
     */
    private fun cancel(state: BuildingDetailScreenState): Job = launch(id = "cancel") {
        if (state.new) {
            router.back()
            return@launch
        }
        val building = state.building?.let { buildingUseCases.observeBuilding(uuid = it.uuid).firstOrNull() }
        updateState { it.showing(building = building).copy(editing = false, equipments = it.storedEquipments, materials = it.storedMaterials) }
    }

    /**
     * Soft deletes the building by setting its deletion and modification times, then leaves the screen.
     *
     * @param state Building Detail Screen state at the time the delete was requested.
     * @return The [Job] representing this execution.
     */
    private fun delete(state: BuildingDetailScreenState): Job = launch(id = "delete") {
        val now = now()
        val building = state.building?.copy(deletedAt = now, modifiedAt = now)
        building?.let { buildingUseCases.upsertBuilding(building = it) }
        router.back()
    }

    /**
     * Clears the deletion of the building, so it is active again, and keeps viewing it.
     *
     * @param state Building Detail Screen state at the time the restore was requested.
     * @return The [Job] representing this execution.
     */
    private fun restore(state: BuildingDetailScreenState): Job = launch(id = "restore") {
        val building = state.building?.copy(deletedAt = null, modifiedAt = now()) ?: return@launch
        buildingUseCases.upsertBuilding(building = building)
        updateState { it.showing(building = building) }
    }

    /**
     * Shows [building], with its coordinates as text.
     *
     * @param building The building to show.
     * @return This state showing [building].
     */
    private fun BuildingDetailScreenState.showing(building: Building?): BuildingDetailScreenState = copy(
        building = building,
        latitude = building?.latitude?.toString().orEmpty(),
        longitude = building?.longitude?.toString().orEmpty()
    )

    /**
     * Rounds this coordinate to six decimal places, about 10 cm.
     *
     * @return The rounded coordinate.
     */
    private fun Double.rounded(): Double = round(x = this * 1_000_000) / 1_000_000

    companion object {
        private const val TAG = "BuildingDetailScreenStore"
    }
}
