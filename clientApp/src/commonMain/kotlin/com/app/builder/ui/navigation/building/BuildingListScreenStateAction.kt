package com.app.builder.ui.navigation.building

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import com.app.builder.ui.component.list.BuildingItem

/** Actions that can be dispatched to a building list store. */
sealed interface BuildingListScreenAction {
    /**
     * Selects a building, either navigating to its detail screen or toggling its selection when in delete mode.
     *
     * @param buildingUuid UUID of the building that was selected.
     */
    data class SelectBuilding(val buildingUuid: String): BuildingListScreenAction
}

/**
 * State of the building list, holding the buildings to display and whether batch delete selection is active.
 *
 * @property buildings Buildings currently displayed.
 */
data class BuildingListScreenState(
    val buildings: ImmutableList<BuildingItem> = persistentListOf(),
)
