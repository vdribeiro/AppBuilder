package com.app.builder.ui.component.equipment

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.app.builder.data.storage.AppFile
import com.app.builder.domain.Equipment
import com.app.builder.domain.gateway.equipment.EquipmentUseCases
import com.app.builder.ui.InjectTranslations
import com.app.builder.ui.Preview
import com.app.builder.ui.component.filters.FiltersAction
import com.app.builder.ui.component.filters.FiltersState
import com.app.builder.ui.component.link.Link
import com.app.builder.ui.component.link.LinkPicker
import com.app.builder.ui.component.link.LinkPickerAction
import com.app.builder.ui.component.link.LinkPickerState
import com.app.builder.ui.component.link.LinkPickerStore
import com.app.builder.ui.component.list.EquipmentItem
import com.app.builder.ui.component.list.EquipmentList
import com.app.builder.ui.store.Store
import com.app.builder.ui.store.previewStore

/**
 * Creates the store of a equipment picker, picking from every equipment that is not deleted, with the equipment list filters.
 *
 * @param equipmentUseCases Use cases used to observe every equipment.
 * @return The equipment picker store.
 */
internal fun equipmentLinkPickerStore(equipmentUseCases: EquipmentUseCases): LinkPickerStore<Equipment, EquipmentItem> = LinkPickerStore(
    state = LinkPickerState(filters = equipmentFilterDefaults),
    candidates = equipmentUseCases.observeEquipments().map { equipments -> equipments.filter { it.deletedAt == null } },
    filters = flow { emitAll(flow = AppFile.EquipmentPreferences.cache().map { it ?: equipmentFilterDefaults }) },
    uuid = { it.uuid },
    toItems = { candidates, filters, picked ->
        candidates.filtered(actionBarData = filters.copy(showDeleted = false)).map { equipment ->
            equipment.toEquipmentItem(visibilityProperties = filters.visibleProperties, selectedUuid = null).copy(checked = equipment.uuid in picked)
        }
    }
)

/**
 * A dialog to tick the equipments to link, shown while the picker is open.
 *
 * @param store Provides the equipment picker state and receives its actions.
 * @param filtersStore The equipment list filters.
 * @param title The title of the dialog, translated.
 * @param onConfirm Callback for when the dialog is saved, with the ticked equipments as links.
 */
@Composable
fun EquipmentLinkPicker(
    store: Store<LinkPickerState<Equipment, EquipmentItem>, LinkPickerAction>,
    filtersStore: Store<FiltersState, FiltersAction>,
    title: String = "",
    onConfirm: (ImmutableList<Link>) -> Unit = {},
) {
    LinkPicker(
        store = store,
        filtersStore = filtersStore,
        tag = "equipment_picker",
        title = title,
        onConfirm = { equipments -> onConfirm(equipments.map { Link(uuid = it.uuid, name = it.name) }.toImmutableList()) },
    ) { items, onClick ->
        EquipmentList(items = items, onClick = { onClick(it.uuid) })
    }
}

@PreviewLightDark
@Composable
private fun EquipmentLinkPickerPreview() = Preview {
    InjectTranslations(translations = mapOf("building_link_equipments_title" to "Link equipments"))
    EquipmentLinkPicker(
        store = previewStore(
            initialState = LinkPickerState(
                open = true,
                items = persistentListOf(EquipmentItem(name = "Air Compressor", checked = true), EquipmentItem(name = "Freight Elevator", checked = false)),
            )
        ),
        filtersStore = previewStore(initialState = FiltersState()),
        title = "building_link_equipments_title",
    )
}
