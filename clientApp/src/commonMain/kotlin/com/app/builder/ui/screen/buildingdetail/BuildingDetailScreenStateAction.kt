package com.app.builder.ui.screen.buildingdetail

import com.app.builder.domain.Building
import com.app.builder.ui.component.bar.ActionBarMode

/** Actions supported by the Building Detail Screen. */
sealed interface BuildingDetailScreenAction {
    /**
     * Updates the building's name.
     *
     * @param name The new name value.
     */
    data class ChangeName(val name: String): BuildingDetailScreenAction
    /**
     * Updates the building's code.
     *
     * @param code The new code value.
     */
    data class ChangeCode(val code: String): BuildingDetailScreenAction
    /**
     * Confirms and commits the current pending mode.
     *
     * @param mode The pending mode.
     */
    data class Ok(val mode: ActionBarMode): BuildingDetailScreenAction
    /**
     * Cancels the current pending mode and returns to the default layout.
     *
     * @param mode The pending mode.
     */
    data class Cancel(val mode: ActionBarMode): BuildingDetailScreenAction
}

/**
 * State of the Building Detail Screen.
 *
 * @property building Building being viewed or edited, or `null` while it's still loading.
 * @property editMode Whether the screen is in edit mode.
 */
data class BuildingDetailScreenState(
    val building: Building? = null,
    val editMode: Boolean = false,
)
