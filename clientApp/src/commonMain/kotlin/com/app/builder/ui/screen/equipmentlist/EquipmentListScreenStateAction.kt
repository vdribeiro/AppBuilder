package com.app.builder.ui.screen.equipmentlist

import kotlin.uuid.Uuid
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import com.app.builder.domain.Equipment
import com.app.builder.ui.component.bar.ActionBarMode
import com.app.builder.ui.component.list.EquipmentItem

/** Actions that can be dispatched to a equipment list store. */
sealed interface EquipmentListScreenAction {
    /**
     * Selects a equipment, either navigating to its detail screen or toggling its selection when in delete mode.
     *
     * @param equipmentUuid UUID of the equipment that was selected.
     */
    data class SelectEquipment(val equipmentUuid: String): EquipmentListScreenAction
    /**
     * Confirms and commits the current pending mode.
     *
     * @param mode The pending mode.
     */
    data class Ok(val mode: ActionBarMode): EquipmentListScreenAction
}

/**
 * State of the equipment list, holding the equipments to display and whether batch delete selection is active.
 *
 * @property equipments Equipments currently displayed.
 * @property entities Equipments the displayed items were built from, kept so an action can read the properties the items hide.
 * @property mode Current action bar mode, controlling how a equipment selection is handled.
 * @property selectedUuids UUIDs of the equipments picked while in batch delete mode.
 */
data class EquipmentListScreenState(
    val equipments: ImmutableList<EquipmentItem> = persistentListOf(),
    val entities: ImmutableList<Equipment> = persistentListOf(),
    val mode: ActionBarMode = ActionBarMode.DEFAULT,
    val selectedUuids: ImmutableList<Uuid> = persistentListOf()
)

/**
 * Slice of [EquipmentListScreenState] the displayed equipment list is derived from, kept separate so unrelated state changes do not re-filter the list.
 *
 * @property selectedUuids UUIDs of the equipments picked while in batch delete mode.
 */
data class EquipmentListFilterCriteria(
    val selectedUuids: ImmutableList<Uuid>
)
