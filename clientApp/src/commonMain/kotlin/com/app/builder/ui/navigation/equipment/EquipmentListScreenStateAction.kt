package com.app.builder.ui.navigation.equipment

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import com.app.builder.ui.component.list.EquipmentItem

/** Actions that can be dispatched to an equipment list store. */
sealed interface EquipmentListScreenAction {
    /**
     * Selects an equipment, either navigating to its detail screen or toggling its selection when in delete mode.
     *
     * @param equipmentUuid UUID of the equipment that was selected.
     */
    data class SelectEquipment(val equipmentUuid: String): EquipmentListScreenAction
}

/**
 * State of the equipment list, holding the equipments to display and whether batch delete selection is active.
 *
 * @property equipments Equipments currently displayed.
 */
data class EquipmentListScreenState(
    val equipments: ImmutableList<EquipmentItem> = persistentListOf(),
)
