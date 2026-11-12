package com.app.builder.ui.navigation.equipment

import kotlin.uuid.Uuid
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import com.app.builder.data.storage.AppFile
import com.app.builder.domain.Building
import com.app.builder.domain.Equipment
import com.app.builder.ui.component.list.BuildingItem
import com.app.builder.ui.navigation.building.buildingFilterDefaults
import com.app.builder.ui.navigation.building.filtered
import com.app.builder.ui.navigation.building.toBuildingItem

/** Actions supported by the Equipment Detail Screen. */
sealed interface EquipmentDetailScreenAction {
    /**
     * Updates the equipment's name.
     *
     * @param name The new name value.
     */
    data class ChangeName(val name: String): EquipmentDetailScreenAction
    /**
     * Updates the equipment's description.
     *
     * @param description The new description value.
     */
    data class ChangeDescription(val description: String): EquipmentDetailScreenAction
    /**
     * Updates the equipment's status.
     *
     * @param status The new status value.
     */
    data class ChangeStatus(val status: Equipment.Status): EquipmentDetailScreenAction
    /**
     * Unlinks a building from the edited equipment.
     *
     * @param buildingUuid The unique identifier of the building.
     */
    data class RemoveBuilding(val buildingUuid: Uuid): EquipmentDetailScreenAction
    /** Opens the building picker with the linked buildings ticked. */
    data object OpenBuildingPicker: EquipmentDetailScreenAction
    /** Closes the building picker and clears the ticked buildings. */
    data object CloseBuildingPicker: EquipmentDetailScreenAction
    /**
     * Ticks or unticks a building in the picker.
     *
     * @param buildingUuid The unique identifier of the building.
     */
    data class ToggleBuildingPick(val buildingUuid: Uuid): EquipmentDetailScreenAction
    /** Sets the buildings linked to the edited equipment to the ticked ones and closes the building picker. */
    data object SaveBuildingPick: EquipmentDetailScreenAction
    /** Start editing the current equipment. */
    data object Edit: EquipmentDetailScreenAction
    /** Save the edited equipment and stop editing it. */
    data object Save: EquipmentDetailScreenAction
    /** Discard the edits, then reload the stored equipment and stop editing it, or leave the screen when the equipment is new. */
    data object Cancel: EquipmentDetailScreenAction
    /** Delete the current equipment. */
    data object Delete: EquipmentDetailScreenAction
    /** Restore the current deleted equipment. */
    data object Restore: EquipmentDetailScreenAction
}

/**
 * State of the Equipment Detail Screen.
 *
 * @property equipment Equipment being viewed or edited, or `null` while it's still loading.
 * @property storedEquipment The equipment as stored, or `null` while it's new or still loading.
 * @property editing Whether the equipment is being edited, which stops it from following the stored equipment.
 * @property new Whether the equipment is new and not stored yet.
 * @property statuses Every equipment status, in selection order.
 * @property buildings The buildings linked to the equipment, including the edits that are not saved yet.
 * @property storedBuildings The buildings linked to the equipment as stored.
 * @property allBuildings Every building that is not deleted.
 * @property buildingFilters The building list filters, applied to the building picker.
 * @property buildingPickerOpen Whether the building picker is open.
 * @property pickedBuildings The buildings ticked in the building picker.
 */
data class EquipmentDetailScreenState(
    val equipment: Equipment? = null,
    val storedEquipment: Equipment? = null,
    val editing: Boolean = false,
    val new: Boolean = false,
    val statuses: ImmutableList<String> = Equipment.Status.entries.map { it.translationKey }.toPersistentList(),
    val buildings: ImmutableList<Building> = persistentListOf(),
    val storedBuildings: ImmutableList<Building> = persistentListOf(),
    val allBuildings: ImmutableList<Building> = persistentListOf(),
    val buildingFilters: AppFile.ActionBarData = buildingFilterDefaults,
    val buildingPickerOpen: Boolean = false,
    val pickedBuildings: ImmutableSet<Uuid> = persistentSetOf(),
) {
    /**
     * The rows of the building picker: [allBuildings] filtered and sorted by [buildingFilters], without the deleted ones, ticked when in [pickedBuildings].
     */
    val buildingPickerItems: ImmutableList<BuildingItem> by lazy {
        allBuildings.filtered(actionBarData = buildingFilters.copy(showDeleted = false)).map { building ->
            building.toBuildingItem(visibilityProperties = buildingFilters.visibleProperties, selectedUuid = null).copy(checked = building.uuid in pickedBuildings)
        }.toImmutableList()
    }
}
