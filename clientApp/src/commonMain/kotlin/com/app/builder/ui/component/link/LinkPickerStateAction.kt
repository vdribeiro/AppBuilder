package com.app.builder.ui.component.link

import kotlin.uuid.Uuid
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import com.app.builder.data.storage.AppFile

/** Actions supported by a link picker. */
sealed interface LinkPickerAction {
    /**
     * Opens the picker.
     *
     * @param picked The unique identifiers of the entities to tick.
     */
    data class Open(val picked: Set<Uuid>): LinkPickerAction
    /**
     * Ticks or unticks an entity.
     *
     * @param uuid The unique identifier of the entity.
     */
    data class Toggle(val uuid: Uuid): LinkPickerAction
    /** Closes the picker and clears the ticked entities. */
    data object Close: LinkPickerAction
}

/**
 * State of a link picker.
 *
 * @param T The type of the entities to pick from.
 * @param Item The type of the rows showing them.
 * @property open Whether the picker is open.
 * @property picked The unique identifiers of the ticked entities.
 * @property candidates Every entity that can be picked.
 * @property filters The list filters applied to the rows.
 * @property items The rows: [candidates] filtered and sorted by [filters], ticked when in [picked].
 * @property pickedCandidates The ticked [candidates].
 */
data class LinkPickerState<T, Item>(
    val open: Boolean = false,
    val picked: ImmutableSet<Uuid> = persistentSetOf(),
    val candidates: ImmutableList<T> = persistentListOf(),
    val filters: AppFile.ActionBarData = AppFile.ActionBarData(),
    val items: ImmutableList<Item> = persistentListOf(),
    val pickedCandidates: ImmutableList<T> = persistentListOf(),
)
