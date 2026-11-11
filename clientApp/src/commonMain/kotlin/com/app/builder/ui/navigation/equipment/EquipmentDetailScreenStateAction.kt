package com.app.builder.ui.navigation.equipment

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import com.app.builder.domain.Equipment

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
 * @property editing Whether the equipment is being edited, which stops it from following the stored equipment.
 * @property new Whether the equipment is new and not stored yet.
 * @property statuses Every equipment status, in selection order.
 */
data class EquipmentDetailScreenState(
    val equipment: Equipment? = null,
    val editing: Boolean = false,
    val new: Boolean = false,
    val statuses: ImmutableList<String> = Equipment.Status.entries.map { it.translationKey }.toPersistentList(),
)
