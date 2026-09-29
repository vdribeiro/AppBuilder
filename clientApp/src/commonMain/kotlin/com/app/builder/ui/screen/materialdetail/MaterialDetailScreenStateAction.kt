package com.app.builder.ui.screen.materialdetail

import com.app.builder.domain.Material
import com.app.builder.ui.component.bar.ActionBarMode

/** Actions supported by the Material Detail Screen. */
sealed interface MaterialDetailScreenAction {
    /**
     * Updates the material's name.
     *
     * @param name The new name value.
     */
    data class ChangeName(val name: String): MaterialDetailScreenAction
    /**
     * Updates the material's code.
     *
     * @param code The new code value.
     */
    data class ChangeCode(val code: String): MaterialDetailScreenAction
    /**
     * Confirms and commits the current pending mode.
     *
     * @param mode The pending mode.
     */
    data class Ok(val mode: ActionBarMode): MaterialDetailScreenAction
    /**
     * Cancels the current pending mode and returns to the default layout.
     *
     * @param mode The pending mode.
     */
    data class Cancel(val mode: ActionBarMode): MaterialDetailScreenAction
}

/**
 * State of the Material Detail Screen.
 *
 * @property material Material being viewed or edited, or `null` while it's still loading.
 * @property editMode Whether the screen is in edit mode.
 */
data class MaterialDetailScreenState(
    val material: Material? = null,
    val editMode: Boolean = false,
)
