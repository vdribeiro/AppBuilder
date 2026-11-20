package com.app.builder.ui.component.building

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.app.builder.data.storage.AppFile
import com.app.builder.domain.Building
import com.app.builder.domain.gateway.building.BuildingUseCases
import com.app.builder.ui.InjectTranslations
import com.app.builder.ui.Preview
import com.app.builder.ui.component.filters.FiltersAction
import com.app.builder.ui.component.filters.FiltersState
import com.app.builder.ui.component.link.Link
import com.app.builder.ui.component.link.LinkPicker
import com.app.builder.ui.component.link.LinkPickerAction
import com.app.builder.ui.component.link.LinkPickerState
import com.app.builder.ui.component.link.LinkPickerStore
import com.app.builder.ui.component.list.BuildingItem
import com.app.builder.ui.component.list.BuildingList
import com.app.builder.ui.store.Store
import com.app.builder.ui.store.previewStore

/**
 * Creates the store of a building picker, picking from every building that is not deleted, with the building list filters.
 *
 * @param buildingUseCases Use cases used to observe every building.
 * @return The building picker store.
 */
internal fun buildingLinkPickerStore(buildingUseCases: BuildingUseCases): LinkPickerStore<Building, BuildingItem> = LinkPickerStore(
    state = LinkPickerState(filters = buildingFilterDefaults),
    candidates = buildingUseCases.observeBuildings().map { buildings -> buildings.filter { it.deletedAt == null } },
    filters = flow { emitAll(flow = AppFile.BuildingPreferences.cache().map { it ?: buildingFilterDefaults }) },
    uuid = { it.uuid },
    toItems = { candidates, filters, picked ->
        candidates.filtered(actionBarData = filters.copy(showDeleted = false)).map { building ->
            building.toBuildingItem(visibilityProperties = filters.visibleProperties, selectedUuid = null).copy(checked = building.uuid in picked)
        }
    }
)

/**
 * A dialog to tick the buildings to link, shown while the picker is open.
 *
 * @param store Provides the building picker state and receives its actions.
 * @param filtersStore The building list filters.
 * @param title The title of the dialog, translated.
 * @param onConfirm Callback for when the dialog is saved, with the ticked buildings as links.
 */
@Composable
fun BuildingLinkPicker(
    store: Store<LinkPickerState<Building, BuildingItem>, LinkPickerAction>,
    filtersStore: Store<FiltersState, FiltersAction>,
    title: String = "",
    onConfirm: (ImmutableList<Link>) -> Unit = {},
) {
    LinkPicker(
        store = store,
        filtersStore = filtersStore,
        tag = "building_picker",
        title = title,
        onConfirm = { buildings -> onConfirm(buildings.map { Link(uuid = it.uuid, name = it.name) }.toImmutableList()) },
    ) { items, onClick ->
        BuildingList(items = items, onClick = { onClick(it.uuid) })
    }
}

@PreviewLightDark
@Composable
private fun BuildingLinkPickerPreview() = Preview {
    InjectTranslations(translations = mapOf("equipment_link_buildings_title" to "Link buildings"))
    BuildingLinkPicker(
        store = previewStore(
            initialState = LinkPickerState(
                open = true,
                items = persistentListOf(BuildingItem(name = "Headquarters", checked = true), BuildingItem(name = "Warehouse", checked = false)),
            )
        ),
        filtersStore = previewStore(initialState = FiltersState()),
        title = "equipment_link_buildings_title",
    )
}
