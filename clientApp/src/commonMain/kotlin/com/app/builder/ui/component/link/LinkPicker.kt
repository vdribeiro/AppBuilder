package com.app.builder.ui.component.link

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.builder.core.security.toUuid
import com.app.builder.ui.InjectTranslations
import com.app.builder.ui.Preview
import com.app.builder.ui.component.filters.FiltersAction
import com.app.builder.ui.component.filters.FiltersState
import com.app.builder.ui.component.filters.StoreFilters
import com.app.builder.ui.component.filters.StoreFiltersButton
import com.app.builder.ui.component.overlay.PickerDialog
import com.app.builder.ui.core.text.Text
import com.app.builder.ui.store.Store
import com.app.builder.ui.store.previewStore

/**
 * A dialog to tick the entities to link, shown while the picker is open, with the list filters of the entities.
 *
 * @param T The type of the entities to pick from.
 * @param Item The type of the rows showing them.
 * @param store Provides the link picker state and receives its actions.
 * @param filtersStore The list filters of the entities.
 * @param tag The test tag of the dialog.
 * @param title The title of the dialog, translated.
 * @param onConfirm Callback for when the dialog is saved, with the ticked entities.
 * @param list Shows the rows, calling its second parameter with the unique identifier of a clicked row.
 */
@Composable
fun <T, Item> LinkPicker(
    store: Store<LinkPickerState<T, Item>, LinkPickerAction>,
    filtersStore: Store<FiltersState, FiltersAction>,
    tag: String = "picker",
    title: String = "",
    onConfirm: (List<T>) -> Unit = {},
    list: @Composable (items: ImmutableList<Item>, onClick: (String) -> Unit) -> Unit,
) {
    val state by store.stateFlow.collectAsStateWithLifecycle()
    if (!state.open) return

    val filters by filtersStore.stateFlow.collectAsStateWithLifecycle()
    PickerDialog(
        tag = tag,
        title = title,
        onSave = {
            onConfirm(state.pickedCandidates)
            store.send(action = LinkPickerAction.Close)
        },
        onDismiss = { store.send(action = LinkPickerAction.Close) },
        actions = { StoreFiltersButton(filtersStore = filtersStore) },
        secondaryActions = if (filters.filtersExpanded) {
            { StoreFilters(filtersStore = filtersStore, showDeletedToggle = false) }
        } else null,
    ) {
        list(state.items) { uuid -> uuid.toUuid()?.let { store.send(action = LinkPickerAction.Toggle(uuid = it)) } }
    }
}

@PreviewLightDark
@Composable
private fun LinkPickerPreview() = Preview {
    InjectTranslations(translations = mapOf("building_link_equipments" to "Link equipments"))
    LinkPicker(
        store = previewStore(
            initialState = LinkPickerState<String, String>(
                open = true,
                items = persistentListOf("Air Compressor", "Freight Elevator"),
            )
        ),
        filtersStore = previewStore(initialState = FiltersState()),
        title = "building_link_equipments",
    ) { items, _ ->
        Column {
            items.forEach { Text(text = it) }
        }
    }
}
