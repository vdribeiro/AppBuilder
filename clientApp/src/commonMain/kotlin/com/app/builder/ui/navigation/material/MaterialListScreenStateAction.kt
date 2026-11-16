package com.app.builder.ui.navigation.material

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import com.app.builder.ui.component.list.MaterialItem

/** Actions that can be dispatched to a material list store. */
sealed interface MaterialListScreenAction {
    /**
     * Selects a material, either navigating to its detail screen or toggling its selection when in delete mode.
     *
     * @param materialUuid UUID of the material that was selected.
     */
    data class SelectMaterial(val materialUuid: String): MaterialListScreenAction
}

/**
 * State of the material list, holding the materials to display and whether batch delete selection is active.
 *
 * @property materials Materials currently displayed.
 */
data class MaterialListScreenState(
    val materials: ImmutableList<MaterialItem> = persistentListOf(),
)
