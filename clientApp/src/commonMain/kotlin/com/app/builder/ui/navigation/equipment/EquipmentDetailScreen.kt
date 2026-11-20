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
import com.app.builder.core.security.toUuid
import com.app.builder.core.security.uuid
import com.app.builder.domain.Building
import com.app.builder.domain.EntityType
import com.app.builder.domain.Equipment
import com.app.builder.domain.User
import com.app.builder.ui.InjectTranslations
import com.app.builder.ui.LocalSplitScreen
import com.app.builder.ui.Preview
import com.app.builder.ui.component.actionbar.ActionBar
import com.app.builder.ui.component.building.BuildingLinkPicker
import com.app.builder.ui.component.card.EquipmentCard
import com.app.builder.ui.component.field.LinkSection
import com.app.builder.ui.component.filters.FiltersAction
import com.app.builder.ui.component.filters.FiltersState
import com.app.builder.ui.component.link.LinkPickerAction
import com.app.builder.ui.component.link.LinkPickerState
import com.app.builder.ui.component.link.toLinkItem
import com.app.builder.ui.component.list.BuildingItem
import com.app.builder.ui.component.list.UserItem
import com.app.builder.ui.component.navigation.Navigation
import com.app.builder.ui.component.navigation.NavigationRoute
import com.app.builder.ui.component.session.SessionState
import com.app.builder.ui.component.user.UserLinkPicker
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
 * @param buildingPickerStore Provides the building picker state and receives its actions.
 * @param userFiltersStore The user list filters, shown in the user picker.
 * @param userPickerStore Provides the user picker state and receives its actions.
 * @param store Provides the equipment detail state and receives its actions.
 */
@Composable
fun EquipmentDetailScreen(
    sessionStore: Store<SessionState, Unit>,
    filtersStore: Store<FiltersState, FiltersAction>,
    buildingFiltersStore: Store<FiltersState, FiltersAction>,
    buildingPickerStore: Store<LinkPickerState<Building, BuildingItem>, LinkPickerAction>,
    userFiltersStore: Store<FiltersState, FiltersAction>,
    userPickerStore: Store<LinkPickerState<User, UserItem>, LinkPickerAction>,
    store: Store<EquipmentDetailScreenState, EquipmentDetailScreenAction>,
) {
    val state by store.stateFlow.collectAsStateWithLifecycle()
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
                editEntityTypes = setOfNotNull(EntityType.BUILDING_EQUIPMENT.takeIf { state.buildingLinks.canEdit }, EntityType.EQUIPMENT_USER.takeIf { state.userLinks.canEdit }),
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
                enabled = equipment != null && state.editing && state.writable,
                uuid = equipment?.uuid?.toString(),
                modifiedAt = equipment?.modifiedAt?.toString(),
                deletedAt = equipment?.deletedAt?.toString(),
                name = equipment?.name,
                onNameChange = { store.send(action = EquipmentDetailScreenAction.ChangeName(name = it)) },
                description = equipment?.description,
                onDescriptionChange = { store.send(action = EquipmentDetailScreenAction.ChangeDescription(description = it)) },
                status = equipment?.status?.translationKey,
                statuses = state.statuses,
                onStatusChange = { key -> Equipment.Status.entries.firstOrNull { it.translationKey == key }?.let { store.send(action = EquipmentDetailScreenAction.ChangeStatus(status = it)) } },
                links = {
                    if (equipment != null && state.buildingLinks.canRead) LinkSection(
                        tag = "equipment_buildings",
                        label = "equipment_buildings",
                        items = state.buildings.map { it.toLinkItem() }.toImmutableList(),
                        editing = state.editing && state.buildingLinks.canEdit,
                        linkLabel = "equipment_link_buildings",
                        onLinkClick = { buildingPickerStore.send(action = LinkPickerAction.Open(picked = state.buildings.map { it.uuid }.toSet())) },
                        onRemove = { item -> item.uuid.toUuid()?.let { store.send(action = EquipmentDetailScreenAction.RemoveBuilding(buildingUuid = it)) } },
                        onClick = { item -> router.navigate(screen = Screen.BuildingDetail(uuid = item.uuid), option = Router.NavOption.ADD) }
                    )
                    if (equipment != null && state.userLinks.canRead) LinkSection(
                        tag = "equipment_users",
                        label = "equipment_users",
                        items = state.users.map { it.toLinkItem() }.toImmutableList(),
                        editing = state.editing && state.userLinks.canEdit,
                        linkLabel = "equipment_link_users",
                        onLinkClick = { userPickerStore.send(action = LinkPickerAction.Open(picked = state.users.map { it.uuid }.toSet())) },
                        onRemove = { item -> item.uuid.toUuid()?.let { store.send(action = EquipmentDetailScreenAction.RemoveUser(userUuid = it)) } },
                        onClick = { item -> router.navigate(screen = Screen.UserDetail(uuid = item.uuid), option = Router.NavOption.ADD) }
                    )
                },
            )
        }
    }

    BuildingLinkPicker(
        store = buildingPickerStore,
        filtersStore = buildingFiltersStore,
        title = "equipment_link_buildings",
        onConfirm = { store.send(action = EquipmentDetailScreenAction.SetBuildings(buildings = it)) }
    )

    UserLinkPicker(
        store = userPickerStore,
        filtersStore = userFiltersStore,
        title = "equipment_link_users",
        onConfirm = { store.send(action = EquipmentDetailScreenAction.SetUsers(users = it)) }
    )
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
        buildingPickerStore = previewStore(initialState = LinkPickerState()),
        userFiltersStore = previewStore(initialState = FiltersState()),
        userPickerStore = previewStore(initialState = LinkPickerState()),
        store = previewStore(
            initialState = EquipmentDetailScreenState(
                equipment = equipment
            )
        )
    )
}
