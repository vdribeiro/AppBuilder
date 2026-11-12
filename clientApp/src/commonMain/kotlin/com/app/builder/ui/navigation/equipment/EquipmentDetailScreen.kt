package com.app.builder.ui.navigation.equipment

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
import com.app.builder.core.config.ClientFlags
import com.app.builder.core.locale.getLocalDateTime
import com.app.builder.core.security.toUuid
import com.app.builder.core.security.uuid
import com.app.builder.domain.EntityType
import com.app.builder.domain.Equipment
import com.app.builder.domain.Permission
import com.app.builder.ui.InjectTranslations
import com.app.builder.ui.LocalSplitScreen
import com.app.builder.ui.Preview
import com.app.builder.ui.component.actionbar.ActionBar
import com.app.builder.ui.component.card.EquipmentCard
import com.app.builder.ui.component.field.LinkItem
import com.app.builder.ui.component.field.LinkSection
import com.app.builder.ui.component.filters.FiltersAction
import com.app.builder.ui.component.filters.FiltersState
import com.app.builder.ui.component.filters.StoreFilters
import com.app.builder.ui.component.filters.StoreFiltersButton
import com.app.builder.ui.component.list.BuildingList
import com.app.builder.ui.component.navigation.Navigation
import com.app.builder.ui.component.navigation.NavigationRoute
import com.app.builder.ui.component.overlay.PickerDialog
import com.app.builder.ui.component.session.SessionState
import com.app.builder.ui.navigation.LocalRouter
import com.app.builder.ui.navigation.Router
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.screen.Screen
import com.app.builder.ui.store.Store
import com.app.builder.ui.store.previewStore

/**
 * The user-facing Equipment Detail Screen, showing a single equipment.
 *
 * @param sessionStore The session store shared by every screen.
 * @param filtersStore The equipment filters shared by every equipment screen, shown when the equipment list is beside this screen.
 * @param buildingFiltersStore The building list filters, shown in the building picker.
 * @param store Provides the equipment detail state and receives its actions.
 */
@Composable
fun EquipmentDetailScreen(
    sessionStore: Store<SessionState, Unit>,
    filtersStore: Store<FiltersState, FiltersAction>,
    buildingFiltersStore: Store<FiltersState, FiltersAction>,
    store: Store<EquipmentDetailScreenState, EquipmentDetailScreenAction>,
) {
    val state by store.stateFlow.collectAsStateWithLifecycle()
    val session by sessionStore.stateFlow.collectAsStateWithLifecycle()
    val account = session.account
    val canEditEquipment = account?.hasPermission(entityType = EntityType.EQUIPMENT, permission = Permission.WRITE) == true
    val canReadBuildings = account?.hasPermission(entityType = EntityType.BUILDING, permission = Permission.READ) == true
    val canReadLinks = ClientFlags.value.buildingEquipments && canReadBuildings && account.hasPermission(entityType = EntityType.BUILDING_EQUIPMENT, permission = Permission.READ)
    val canEditLinks = ClientFlags.value.buildingEquipments && canReadBuildings && account.hasPermission(entityType = EntityType.BUILDING_EQUIPMENT, permission = Permission.WRITE)
    val equipment = state.equipment
    val deleted = equipment?.deletedAt != null

    val router = LocalRouter.current
    val splitScreen = LocalSplitScreen.current

    Screen(
        onBackClick = { router.back() },
        topBar = {
            ActionBar(
                sessionStore = sessionStore,
                filtersStore = filtersStore.takeIf { splitScreen },
                entityType = EntityType.EQUIPMENT,
                editEntityTypes = if (canEditLinks) setOf(EntityType.BUILDING_EQUIPMENT) else emptySet(),
                deleted = deleted,
                onAddClick = if (splitScreen && !state.editing) {
                    { router.navigate(screen = Screen.EquipmentDetail(uuid = uuid().toString(), new = true), option = Router.NavOption.REPLACE_LAST) }
                } else null,
                onEditClick = if (equipment != null && !state.editing && !deleted) {
                    { store.send(action = EquipmentDetailScreenAction.Edit) }
                } else null,
                onSaveClick = if (state.editing) {
                    { store.send(action = EquipmentDetailScreenAction.Save) }
                } else null,
                onCancelClick = if (state.editing) {
                    { store.send(action = EquipmentDetailScreenAction.Cancel) }
                } else null,
                onDeleteClick = if (equipment != null && !state.editing) {
                    { store.send(action = if (it) EquipmentDetailScreenAction.Delete else EquipmentDetailScreenAction.Restore) }
                } else null,
            )
        },
        bottomBar = { Navigation(sessionStore = sessionStore, selected = NavigationRoute.EQUIPMENT) },
    ) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(state = rememberScrollState())) {
            EquipmentCard(
                enabled = equipment != null && state.editing && canEditEquipment,
                uuid = equipment?.uuid?.toString(),
                modifiedAt = equipment?.modifiedAt?.let { getLocalDateTime(utc = it.toString()) },
                deletedAt = equipment?.deletedAt?.let { getLocalDateTime(utc = it.toString()) },
                name = equipment?.name,
                onNameChange = { store.send(action = EquipmentDetailScreenAction.ChangeName(name = it)) },
                description = equipment?.description,
                onDescriptionChange = { store.send(action = EquipmentDetailScreenAction.ChangeDescription(description = it)) },
                status = equipment?.status?.translationKey,
                statuses = state.statuses,
                onStatusChange = { key -> Equipment.Status.entries.firstOrNull { it.translationKey == key }?.let { store.send(action = EquipmentDetailScreenAction.ChangeStatus(status = it)) } },
                links = {
                    if (equipment != null && canReadLinks) LinkSection(
                        tag = "equipment_buildings",
                        label = "equipment_buildings",
                        items = state.buildings.map { LinkItem(uuid = it.uuid.toString(), name = it.name) }.toImmutableList(),
                        editing = state.editing && canEditLinks,
                        linkLabel = "equipment_link_buildings",
                        onLinkClick = { store.send(action = EquipmentDetailScreenAction.OpenBuildingPicker) },
                        onRemove = { item -> item.uuid.toUuid()?.let { store.send(action = EquipmentDetailScreenAction.RemoveBuilding(buildingUuid = it)) } },
                        onClick = { item -> router.navigate(screen = Screen.BuildingDetail(uuid = item.uuid), option = Router.NavOption.ADD) }
                    )
                },
            )
        }
    }

    if (state.buildingPickerOpen) {
        val buildingFilters by buildingFiltersStore.stateFlow.collectAsStateWithLifecycle()
        PickerDialog(
            tag = "building_picker",
            title = "equipment_link_buildings",
            onSave = { store.send(action = EquipmentDetailScreenAction.SaveBuildingPick) },
            onDismiss = { store.send(action = EquipmentDetailScreenAction.CloseBuildingPicker) },
            actions = { StoreFiltersButton(filtersStore = buildingFiltersStore) },
            secondaryActions = if (buildingFilters.filtersExpanded) {
                { StoreFilters(filtersStore = buildingFiltersStore, showDeletedToggle = false) }
            } else null,
        ) {
            BuildingList(
                items = state.buildingPickerItems,
                onClick = { item -> item.uuid.toUuid()?.let { store.send(action = EquipmentDetailScreenAction.ToggleBuildingPick(buildingUuid = it)) } }
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun EquipmentDetailScreenPreview() = Preview {
    InjectTranslations(
        translations = mapOf(
            "equipment_name" to "Name"
        )
    )
    val equipment = Equipment(
        name = "Big Compressor",
        description = "A description lalala"
    )
    EquipmentDetailScreen(
        sessionStore = previewStore(initialState = SessionState()),
        filtersStore = previewStore(initialState = FiltersState()),
        buildingFiltersStore = previewStore(initialState = FiltersState()),
        store = previewStore(
            initialState = EquipmentDetailScreenState(
                equipment = equipment
            )
        )
    )
}
