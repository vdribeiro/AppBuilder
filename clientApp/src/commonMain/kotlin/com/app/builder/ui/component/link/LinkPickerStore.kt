package com.app.builder.ui.component.link

import kotlin.uuid.Uuid
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import com.app.builder.data.storage.AppFile
import com.app.builder.ui.store.Store

/**
 * Store backing a link picker, observing the entities to pick from and their list filters, and keeping the ticked ones.
 *
 * @param T The type of the entities to pick from.
 * @param Item The type of the rows showing them.
 * @param state Initial link picker state.
 * @property candidates Every entity that can be picked.
 * @property filters The list filters of the entities.
 * @property uuid Gets the unique identifier of an entity.
 * @property toItems Builds the rows from the entities, their list filters and the unique identifiers of the ticked ones.
 */
class LinkPickerStore<T, Item>(
    state: LinkPickerState<T, Item>,
    private val candidates: Flow<List<T>>,
    private val filters: Flow<AppFile.ActionBarData>,
    private val uuid: (T) -> Uuid,
    private val toItems: (candidates: List<T>, filters: AppFile.ActionBarData, picked: Set<Uuid>) -> List<Item>,
): Store<LinkPickerState<T, Item>, LinkPickerAction>(initialState = state) {
    init {
        setup()
    }

    /**
     * Observes the entities to pick from and their list filters.
     *
     * @return The [Job] representing this execution.
     */
    private fun setup(): Job = launch(id = "setup") {
        candidates.observe(id = "candidates") { candidates ->
            updateState { it.copy(candidates = candidates.toImmutableList()).derived() }
        }
        filters.observe(id = "filters") { filters ->
            updateState { it.copy(filters = filters).derived() }
        }
    }

    override fun reducer(state: LinkPickerState<T, Item>, action: LinkPickerAction) {
        super.reducer(state = state, action = action)
        when (action) {
            is LinkPickerAction.Open -> updateState { it.copy(open = true, picked = action.picked.toPersistentSet()).derived() }
            is LinkPickerAction.Toggle -> updateState { current ->
                val picked = if (action.uuid in current.picked) current.picked - action.uuid else current.picked + action.uuid
                current.copy(picked = picked.toPersistentSet()).derived()
            }

            LinkPickerAction.Close -> updateState { it.copy(open = false, picked = persistentSetOf()).derived() }
        }
    }

    /**
     * Rebuilds [LinkPickerState.items] and [LinkPickerState.pickedCandidates] from the rest of this state.
     *
     * @return This state with both rebuilt.
     */
    private fun LinkPickerState<T, Item>.derived(): LinkPickerState<T, Item> = copy(
        items = toItems(candidates, filters, picked).toImmutableList(),
        pickedCandidates = candidates.filter { uuid(it) in picked }.toImmutableList()
    )
}
