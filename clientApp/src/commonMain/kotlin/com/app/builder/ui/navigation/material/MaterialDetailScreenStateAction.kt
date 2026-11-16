package com.app.builder.ui.navigation.material

import kotlin.uuid.Uuid
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import com.app.builder.domain.Material
import com.app.builder.ui.component.link.Link
import com.app.builder.ui.component.link.LinkPermissions

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
    /**
     * Sets the buildings linked to the edited material, keeping the quantities already typed and starting the others at one.
     *
     * @param buildings The buildings to link.
     */
    data class SetBuildings(val buildings: ImmutableList<Link>): MaterialDetailScreenAction
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
 * @property writable Whether the current user can edit, delete and restore the material.
 * @property buildingLinks What the current user can do with the links to the buildings of the material.
 */
data class MaterialDetailScreenState(
    val material: Material? = null,
    val storedMaterial: Material? = null,
    val editing: Boolean = false,
    val new: Boolean = false,
    val buildings: ImmutableList<Link> = persistentListOf(),
    val storedBuildings: ImmutableList<Link> = persistentListOf(),
    val writable: Boolean = false,
    val buildingLinks: LinkPermissions = LinkPermissions(),
)
