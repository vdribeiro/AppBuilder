package com.app.builder.ui.screen.equipmentdetail

import com.app.builder.domain.Equipment
import com.app.builder.ui.component.bar.ActionBarMode

/** Actions supported by the Equipment Detail Screen. */
sealed interface EquipmentDetailScreenAction {
    /**
     * Updates the equipment's name.
     *
     * @param name The new name value.
     */
    data class ChangeName(val name: String): EquipmentDetailScreenAction
    /**
     * Updates the equipment's code.
     *
     * @param code The new code value.
     */
    data class ChangeCode(val code: String): EquipmentDetailScreenAction
    /**
     * Confirms and commits the current pending mode.
     *
     * @param mode The pending mode.
     */
    data class Ok(val mode: ActionBarMode): EquipmentDetailScreenAction
    /**
     * Cancels the current pending mode and returns to the default layout.
     *
     * @param mode The pending mode.
     */
    data class Cancel(val mode: ActionBarMode): EquipmentDetailScreenAction
}

/**
 * State of the Equipment Detail Screen.
 *
 * @property equipment Equipment being viewed or edited, or `null` while it's still loading.
 * @property editMode Whether the screen is in edit mode.
 */
data class EquipmentDetailScreenState(
    val equipment: Equipment? = null,
    val editMode: Boolean = false,
)
