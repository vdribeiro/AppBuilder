package com.app.builder.ui.screen.buildinglist

import kotlin.uuid.Uuid
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import com.app.builder.domain.Building
import com.app.builder.ui.component.bar.ActionBarMode
import com.app.builder.ui.component.list.BuildingItem

/** Actions that can be dispatched to a building list store. */
sealed interface BuildingListScreenAction {
    /**
     * Selects a building, either navigating to its detail screen or toggling its selection when in delete mode.
     *
     * @param buildingUuid UUID of the building that was selected.
     */
    data class SelectBuilding(val buildingUuid: String): BuildingListScreenAction
    /**
     * Confirms and commits the current pending mode.
     *
     * @param mode The pending mode.
     */
    data class Ok(val mode: ActionBarMode): BuildingListScreenAction
}

/**
 * State of the building list, holding the buildings to display and whether batch delete selection is active.
 *
 * @property buildings Buildings currently displayed.
 * @property entities Buildings the displayed items were built from, kept so an action can read the properties the items hide.
 * @property mode Current action bar mode, controlling how a building selection is handled.
 * @property selectedUuids UUIDs of the buildings picked while in batch delete mode.
 */
data class BuildingListScreenState(
    val buildings: ImmutableList<BuildingItem> = persistentListOf(),
    val entities: ImmutableList<Building> = persistentListOf(),
    val mode: ActionBarMode = ActionBarMode.DEFAULT,
    val selectedUuids: ImmutableList<Uuid> = persistentListOf()
)

/**
 * Slice of [BuildingListScreenState] the displayed building list is derived from, kept separate so unrelated state changes do not re-filter the list.
 *
 * @property selectedUuids UUIDs of the buildings picked while in batch delete mode.
 */
data class BuildingListFilterCriteria(
    val selectedUuids: ImmutableList<Uuid>
)
