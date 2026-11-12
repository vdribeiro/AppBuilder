package com.app.builder.ui.navigation.building

import kotlin.uuid.Uuid
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import com.app.builder.data.storage.AppFile
import com.app.builder.domain.Building
import com.app.builder.domain.Equipment
import com.app.builder.ui.component.list.EquipmentItem
import com.app.builder.ui.navigation.equipment.equipmentFilterDefaults
import com.app.builder.ui.navigation.equipment.filtered
import com.app.builder.ui.navigation.equipment.toEquipmentItem

/** Actions supported by the Building Detail Screen. */
sealed interface BuildingDetailScreenAction {
    /**
     * Updates the building's name.
     *
     * @param name The new name value.
     */
    data class ChangeName(val name: String): BuildingDetailScreenAction
    /**
     * Updates the building's description.
     *
     * @param description The new description value.
     */
    data class ChangeDescription(val description: String): BuildingDetailScreenAction
    /**
     * Updates the building's address.
     *
     * @param address The new address value.
     */
    data class ChangeAddress(val address: String): BuildingDetailScreenAction
    /**
     * Updates the typed latitude.
     *
     * @param latitude The new latitude text.
     */
    data class ChangeLatitude(val latitude: String): BuildingDetailScreenAction
    /**
     * Updates the typed longitude.
     *
     * @param longitude The new longitude text.
     */
    data class ChangeLongitude(val longitude: String): BuildingDetailScreenAction
    /**
     * Fills both coordinates, such as with the device location.
     *
     * @param latitude The new latitude in degrees.
     * @param longitude The new longitude in degrees.
     */
    data class ChangeCoordinates(val latitude: Double, val longitude: Double): BuildingDetailScreenAction
    /** Opens the equipment picker with the linked equipments ticked. */
    data object OpenEquipmentPicker: BuildingDetailScreenAction
    /** Closes the equipment picker and clears the ticked equipments. */
    data object CloseEquipmentPicker: BuildingDetailScreenAction
    /**
     * Ticks or unticks an equipment in the picker.
     *
     * @param equipmentUuid The unique identifier of the equipment.
     */
    data class ToggleEquipmentPick(val equipmentUuid: Uuid): BuildingDetailScreenAction
    /** Sets the equipments linked to the edited building to the ticked ones and closes the equipment picker. */
    data object SaveEquipmentPick: BuildingDetailScreenAction
    /**
     * Unlinks an equipment from the edited building.
     *
     * @param equipmentUuid The unique identifier of the equipment.
     */
    data class RemoveEquipment(val equipmentUuid: Uuid): BuildingDetailScreenAction
    /** Start editing the current building. */
    data object Edit: BuildingDetailScreenAction
    /** Save the edited building and stop editing it. */
    data object Save: BuildingDetailScreenAction
    /** Discard the edits, then reload the stored building and stop editing it, or leave the screen when the building is new. */
    data object Cancel: BuildingDetailScreenAction
    /** Delete the current building. */
    data object Delete: BuildingDetailScreenAction
    /** Restore the current deleted building. */
    data object Restore: BuildingDetailScreenAction
}

/**
 * State of the Building Detail Screen.
 *
 * @property building Building being viewed or edited, or `null` while it's still loading.
 * @property storedBuilding The building as stored, or `null` while it's new or still loading.
 * @property editing Whether the building is being edited, which stops it from following the stored building.
 * @property new Whether the building is new and not stored yet.
 * @property latitude The latitude as typed, empty when the building has no coordinates.
 * @property longitude The longitude as typed, empty when the building has no coordinates.
 * @property equipments The equipments linked to the building, including the edits that are not saved yet.
 * @property storedEquipments The equipments linked to the building as stored.
 * @property allEquipments Every equipment that is not deleted.
 * @property equipmentFilters The equipment list filters, applied to the equipment picker.
 * @property equipmentPickerOpen Whether the equipment picker is open.
 * @property pickedEquipments The equipments ticked in the equipment picker.
 */
data class BuildingDetailScreenState(
    val building: Building? = null,
    val storedBuilding: Building? = null,
    val editing: Boolean = false,
    val new: Boolean = false,
    val latitude: String = "",
    val longitude: String = "",
    val equipments: ImmutableList<Equipment> = persistentListOf(),
    val storedEquipments: ImmutableList<Equipment> = persistentListOf(),
    val allEquipments: ImmutableList<Equipment> = persistentListOf(),
    val equipmentFilters: AppFile.ActionBarData = equipmentFilterDefaults,
    val equipmentPickerOpen: Boolean = false,
    val pickedEquipments: ImmutableSet<Uuid> = persistentSetOf(),
) {
    /**
     * The rows of the equipment picker: [allEquipments] filtered and sorted by [equipmentFilters], without the deleted ones, ticked when in [pickedEquipments].
     */
    val equipmentPickerItems: ImmutableList<EquipmentItem> by lazy {
        allEquipments.filtered(actionBarData = equipmentFilters.copy(showDeleted = false)).map { equipment ->
            equipment.toEquipmentItem(visibilityProperties = equipmentFilters.visibleProperties, selectedUuid = null).copy(checked = equipment.uuid in pickedEquipments)
        }.toImmutableList()
    }

    /** Whether [latitude] is not a number within ±90, or is missing while [longitude] is set. */
    val latitudeError: Boolean
        get() = !latitude.isCoordinate(limit = 90.0) || (latitude.isBlank() && longitude.isNotBlank())

    /** Whether [longitude] is not a number within ±180, or is missing while [latitude] is set. */
    val longitudeError: Boolean
        get() = !longitude.isCoordinate(limit = 180.0) || (longitude.isBlank() && latitude.isNotBlank())
}

/**
 * Checks whether this text is empty or a number within ±[limit].
 *
 * @param limit The largest absolute value allowed.
 * @return `true` if the text is blank or a valid coordinate.
 */
private fun String.isCoordinate(limit: Double): Boolean = isBlank() || toDoubleOrNull()?.let { it in -limit..limit } == true
