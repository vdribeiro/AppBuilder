package com.app.builder.ui.component.material

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.app.builder.data.storage.AppFile
import com.app.builder.domain.Material
import com.app.builder.domain.gateway.material.MaterialUseCases
import com.app.builder.ui.InjectTranslations
import com.app.builder.ui.Preview
import com.app.builder.ui.component.filters.FiltersAction
import com.app.builder.ui.component.filters.FiltersState
import com.app.builder.ui.component.link.Link
import com.app.builder.ui.component.link.LinkPicker
import com.app.builder.ui.component.link.LinkPickerAction
import com.app.builder.ui.component.link.LinkPickerState
import com.app.builder.ui.component.link.LinkPickerStore
import com.app.builder.ui.component.list.MaterialItem
import com.app.builder.ui.component.list.MaterialList
import com.app.builder.ui.store.Store
import com.app.builder.ui.store.previewStore

/**
 * Creates the store of a material picker, picking from every material that is not deleted, with the material list filters.
 *
 * @param materialUseCases Use cases used to observe every material.
 * @return The material picker store.
 */
internal fun materialLinkPickerStore(materialUseCases: MaterialUseCases): LinkPickerStore<Material, MaterialItem> = LinkPickerStore(
    state = LinkPickerState(filters = materialFilterDefaults),
    candidates = materialUseCases.observeMaterials().map { materials -> materials.filter { it.deletedAt == null } },
    filters = flow { emitAll(flow = AppFile.MaterialPreferences.cache().map { it ?: materialFilterDefaults }) },
    uuid = { it.uuid },
    toItems = { candidates, filters, picked ->
        candidates.filtered(actionBarData = filters.copy(showDeleted = false)).map { material ->
            material.toMaterialItem(visibilityProperties = filters.visibleProperties, selectedUuid = null).copy(checked = material.uuid in picked)
        }
    }
)

/**
 * A dialog to tick the materials to link, shown while the picker is open.
 *
 * @param store Provides the material picker state and receives its actions.
 * @param filtersStore The material list filters.
 * @param title The title of the dialog, translated.
 * @param onConfirm Callback for when the dialog is saved, with the ticked materials as links.
 */
@Composable
fun MaterialLinkPicker(
    store: Store<LinkPickerState<Material, MaterialItem>, LinkPickerAction>,
    filtersStore: Store<FiltersState, FiltersAction>,
    title: String = "",
    onConfirm: (ImmutableList<Link>) -> Unit = {},
) {
    LinkPicker(
        store = store,
        filtersStore = filtersStore,
        tag = "material_picker",
        title = title,
        onConfirm = { materials -> onConfirm(materials.map { Link(uuid = it.uuid, name = it.name) }.toImmutableList()) },
    ) { items, onClick ->
        MaterialList(items = items, onClick = { onClick(it.uuid) })
    }
}

@PreviewLightDark
@Composable
private fun MaterialLinkPickerPreview() = Preview {
    InjectTranslations(translations = mapOf("building_link_materials" to "Link materials"))
    MaterialLinkPicker(
        store = previewStore(
            initialState = LinkPickerState(
                open = true,
                items = persistentListOf(MaterialItem(name = "Copper Cable", checked = true), MaterialItem(name = "Cement", checked = false)),
            )
        ),
        filtersStore = previewStore(initialState = FiltersState()),
        title = "building_link_materials",
    )
}
