package com.app.builder.ui.screen.materiallist

import kotlin.uuid.Uuid
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import com.app.builder.domain.Material
import com.app.builder.ui.component.bar.ActionBarMode
import com.app.builder.ui.component.list.MaterialItem

/** Actions that can be dispatched to a material list store. */
sealed interface MaterialListScreenAction {
    /**
     * Selects a material, either navigating to its detail screen or toggling its selection when in delete mode.
     *
     * @param materialUuid UUID of the material that was selected.
     */
    data class SelectMaterial(val materialUuid: String): MaterialListScreenAction
    /**
     * Confirms and commits the current pending mode.
     *
     * @param mode The pending mode.
     */
    data class Ok(val mode: ActionBarMode): MaterialListScreenAction
}

/**
 * State of the material list, holding the materials to display and whether batch delete selection is active.
 *
 * @property materials Materials currently displayed.
 * @property entities Materials the displayed items were built from, kept so an action can read the properties the items hide.
 * @property mode Current action bar mode, controlling how a material selection is handled.
 * @property selectedUuids UUIDs of the materials picked while in batch delete mode.
 */
data class MaterialListScreenState(
    val materials: ImmutableList<MaterialItem> = persistentListOf(),
    val entities: ImmutableList<Material> = persistentListOf(),
    val mode: ActionBarMode = ActionBarMode.DEFAULT,
    val selectedUuids: ImmutableList<Uuid> = persistentListOf()
)

/**
 * Slice of [MaterialListScreenState] the displayed material list is derived from, kept separate so unrelated state changes do not re-filter the list.
 *
 * @property selectedUuids UUIDs of the materials picked while in batch delete mode.
 */
data class MaterialListFilterCriteria(
    val selectedUuids: ImmutableList<Uuid>
)
