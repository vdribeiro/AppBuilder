package com.app.builder.ui.navigation.material

import kotlinx.collections.immutable.toImmutableList
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.builder.core.security.toUuid
import com.app.builder.core.security.uuid
import com.app.builder.domain.Building
import com.app.builder.domain.EntityType
import com.app.builder.domain.Material
import com.app.builder.ui.InjectTranslations
import com.app.builder.ui.LocalSplitScreen
import com.app.builder.ui.Preview
import com.app.builder.ui.component.actionbar.ActionBar
import com.app.builder.ui.component.building.BuildingLinkPicker
import com.app.builder.ui.component.card.MaterialCard
import com.app.builder.ui.component.field.LinkExtra
import com.app.builder.ui.component.field.LinkSection
import com.app.builder.ui.component.filters.FiltersAction
import com.app.builder.ui.component.filters.FiltersState
import com.app.builder.ui.component.link.LinkPickerAction
import com.app.builder.ui.component.link.LinkPickerState
import com.app.builder.ui.component.link.toLinkItem
import com.app.builder.ui.component.list.BuildingItem
import com.app.builder.ui.component.navigation.Navigation
import com.app.builder.ui.component.navigation.NavigationRoute
import com.app.builder.ui.component.session.SessionState
import com.app.builder.ui.navigation.LocalRouter
import com.app.builder.ui.navigation.Router
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.screen.Screen
import com.app.builder.ui.store.Store
import com.app.builder.ui.store.previewStore

/**
 * The user-facing Material Detail Screen, showing a single material.
 *
 * @param sessionStore The session store shared by every screen.
 * @param filtersStore The material filters shared by every material screen, shown when the material list is beside this screen.
 * @param buildingFiltersStore The building list filters, shown in the building picker.
 * @param buildingPickerStore Provides the building picker state and receives its actions.
 * @param store Provides the material detail state and receives its actions.
 */
@Composable
fun MaterialDetailScreen(
    sessionStore: Store<SessionState, Unit>,
    filtersStore: Store<FiltersState, FiltersAction>,
    buildingFiltersStore: Store<FiltersState, FiltersAction>,
    buildingPickerStore: Store<LinkPickerState<Building, BuildingItem>, LinkPickerAction>,
    store: Store<MaterialDetailScreenState, MaterialDetailScreenAction>,
) {
    val state by store.stateFlow.collectAsStateWithLifecycle()
    val material = state.material
    val deleted = material?.deletedAt != null

    val router = LocalRouter.current
    val splitScreen = LocalSplitScreen.current

    Screen(
        onBackClick = { router.back() },
        topBar = {
            ActionBar(
                sessionStore = sessionStore,
                filtersStore = filtersStore.takeIf { splitScreen },
                entityType = EntityType.MATERIAL,
                editEntityTypes = if (state.buildingLinks.canEdit) setOf(EntityType.BUILDING_MATERIAL) else emptySet(),
                deleted = deleted,
                onAddClick = if (splitScreen && !state.editing) {
                    { router.navigate(screen = Screen.MaterialDetail(uuid = uuid().toString(), new = true), option = Router.NavOption.REPLACE_LAST) }
                } else null,
                onEditClick = if (material != null && !state.editing && !deleted) {
                    { store.send(action = MaterialDetailScreenAction.Edit) }
                } else null,
                onSaveClick = if (state.editing) {
                    { store.send(action = MaterialDetailScreenAction.Save) }
                } else null,
                onCancelClick = if (state.editing) {
                    { store.send(action = MaterialDetailScreenAction.Cancel) }
                } else null,
                onDeleteClick = if (material != null && !state.editing) {
                    { store.send(action = if (it) MaterialDetailScreenAction.Delete else MaterialDetailScreenAction.Restore) }
                } else null,
            )
        },
        bottomBar = { Navigation(sessionStore = sessionStore, selected = NavigationRoute.MATERIAL) },
    ) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(state = rememberScrollState())) {
            MaterialCard(
                enabled = material != null && state.editing && state.writable,
                uuid = material?.uuid?.toString(),
                modifiedAt = material?.modifiedAt?.toString(),
                deletedAt = material?.deletedAt?.toString(),
                name = material?.name,
                onNameChange = { store.send(action = MaterialDetailScreenAction.ChangeName(name = it)) },
                description = material?.description,
                onDescriptionChange = { store.send(action = MaterialDetailScreenAction.ChangeDescription(description = it)) },
                links = {
                    if (material != null && state.buildingLinks.canRead) LinkSection(
                        tag = "material_buildings",
                        label = "material_buildings",
                        items = state.buildings.map { it.toLinkItem() }.toImmutableList(),
                        editing = state.editing && state.buildingLinks.canEdit,
                        linkLabel = "material_link_buildings",
                        onLinkClick = { buildingPickerStore.send(action = LinkPickerAction.Open(picked = state.buildings.map { it.uuid }.toSet())) },
                        onRemove = { item -> item.uuid.toUuid()?.let { store.send(action = MaterialDetailScreenAction.RemoveBuilding(buildingUuid = it)) } },
                        onExtraChange = { item, extra ->
                            when (extra) {
                                is LinkExtra.Number -> item.uuid.toUuid()?.let { store.send(action = MaterialDetailScreenAction.ChangeBuildingQuantity(buildingUuid = it, quantity = extra.value)) }
                            }
                        },
                        onClick = { item -> router.navigate(screen = Screen.BuildingDetail(uuid = item.uuid), option = Router.NavOption.ADD) }
                    )
                },
            )
        }
    }

    BuildingLinkPicker(
        store = buildingPickerStore,
        filtersStore = buildingFiltersStore,
        title = "material_link_buildings_title",
        onConfirm = { store.send(action = MaterialDetailScreenAction.SetBuildings(buildings = it)) }
    )
}

@PreviewLightDark
@Composable
private fun MaterialDetailScreenPreview() = Preview {
    InjectTranslations(
        translations = mapOf(
            "material_name" to "Name"
        )
    )
    val material = Material(
        name = "Copper Cable",
        description = "2.5 mm² single core"
    )
    MaterialDetailScreen(
        sessionStore = previewStore(initialState = SessionState()),
        filtersStore = previewStore(initialState = FiltersState()),
        buildingFiltersStore = previewStore(initialState = FiltersState()),
        buildingPickerStore = previewStore(initialState = LinkPickerState()),
        store = previewStore(
            initialState = MaterialDetailScreenState(
                material = material
            )
        )
    )
}
