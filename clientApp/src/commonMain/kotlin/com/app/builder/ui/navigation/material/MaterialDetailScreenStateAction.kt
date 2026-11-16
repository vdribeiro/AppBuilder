package com.app.builder.ui.navigation.material

import kotlin.uuid.Uuid
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import com.app.builder.data.storage.AppFile
import com.app.builder.domain.Building
import com.app.builder.domain.Material
import com.app.builder.domain.gateway.buildingmaterial.BuildingOfMaterial
import com.app.builder.ui.component.list.BuildingItem
import com.app.builder.ui.navigation.building.buildingFilterDefaults
import com.app.builder.ui.navigation.building.filtered
import com.app.builder.ui.navigation.building.toBuildingItem

/** Actions supported by the Material Detail Screen. */
sealed interface MaterialDetailScreenAction {
    /**
     * Updates the material's name.
     *
     * @param name The new name value.
     */
    data class ChangeName(val name: String): MaterialDetailScreenAction
    /**
     * Updates the material's description.
     *
     * @param description The new description value.
     */
    data class ChangeDescription(val description: String): MaterialDetailScreenAction
    /**
     * Updates the typed quantity of the edited material stored in a building.
     *
     * @param buildingUuid The unique identifier of the building.
     * @param quantity The new quantity text.
     */
    data class ChangeBuildingQuantity(val buildingUuid: Uuid, val quantity: String): MaterialDetailScreenAction
    /**
     * Unlinks a building from the edited material.
     *
     * @param buildingUuid The unique identifier of the building.
     */
    data class RemoveBuilding(val buildingUuid: Uuid): MaterialDetailScreenAction
    /** Opens the building picker with the linked buildings ticked. */
    data object OpenBuildingPicker: MaterialDetailScreenAction
    /** Closes the building picker and clears the ticked buildings. */
    data object CloseBuildingPicker: MaterialDetailScreenAction
    /**
     * Ticks or unticks a building in the picker.
     *
     * @param buildingUuid The unique identifier of the building.
     */
    data class ToggleBuildingPick(val buildingUuid: Uuid): MaterialDetailScreenAction
    /** Sets the buildings linked to the edited material to the ticked ones, keeping the quantities already typed, and closes the building picker. */
    data object SaveBuildingPick: MaterialDetailScreenAction
    /** Start editing the current material. */
    data object Edit: MaterialDetailScreenAction
    /** Save the edited material and stop editing it. */
    data object Save: MaterialDetailScreenAction
    /** Discard the edits, then reload the stored material and stop editing it, or leave the screen when the material is new. */
    data object Cancel: MaterialDetailScreenAction
    /** Delete the current material. */
    data object Delete: MaterialDetailScreenAction
    /** Restore the current deleted material. */
    data object Restore: MaterialDetailScreenAction
}

/**
 * State of the Material Detail Screen.
 *
 * @property material Material being viewed or edited, or `null` while it's still loading.
 * @property storedMaterial The material as stored, or `null` while it's new or still loading.
 * @property editing Whether the material is being edited, which stops it from following the stored material.
 * @property new Whether the material is new and not stored yet.
 * @property buildings The buildings linked to the material with their quantities as typed, including the edits that are not saved yet.
 * @property storedBuildings The buildings linked to the material with their quantities, as stored.
 * @property allBuildings Every building that is not deleted.
 * @property buildingFilters The building list filters, applied to the building picker.
 * @property buildingPickerOpen Whether the building picker is open.
 * @property pickedBuildings The buildings ticked in the building picker.
 */
data class MaterialDetailScreenState(
    val material: Material? = null,
    val storedMaterial: Material? = null,
    val editing: Boolean = false,
    val new: Boolean = false,
    val buildings: ImmutableList<BuildingLink> = persistentListOf(),
    val storedBuildings: ImmutableList<BuildingOfMaterial> = persistentListOf(),
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

/**
 * A building linked to the material being viewed or edited.
 *
 * @property building The linked building.
 * @property quantity The quantity of the material stored in the building, as typed.
 */
data class BuildingLink(
    val building: Building,
    val quantity: String = "1"
) {
    /** Whether [quantity] is not a whole number of at least one. */
    val quantityError: Boolean
        get() = quantity.toIntOrNull()?.let { it >= 1 } != true
}

/**
 * Converts this stored building of a material into a [BuildingLink], with its quantity as text.
 *
 * @return The corresponding [BuildingLink].
 */
internal fun BuildingOfMaterial.toBuildingLink(): BuildingLink = BuildingLink(building = building, quantity = quantity.toString())
