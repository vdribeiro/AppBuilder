package com.app.builder.ui.navigation.building

import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.builder.core.security.toUuid
import com.app.builder.core.security.uuid
import com.app.builder.domain.Building
import com.app.builder.domain.EntityType
import com.app.builder.domain.Equipment
import com.app.builder.domain.Material
import com.app.builder.domain.User
import com.app.builder.ui.InjectTranslations
import com.app.builder.ui.LocalSplitScreen
import com.app.builder.ui.Preview
import com.app.builder.ui.component.actionbar.ActionBar
import com.app.builder.ui.component.card.BuildingCard
import com.app.builder.ui.component.equipment.EquipmentLinkPicker
import com.app.builder.ui.component.field.LinkExtra
import com.app.builder.ui.component.field.LinkSection
import com.app.builder.ui.component.filters.FiltersAction
import com.app.builder.ui.component.filters.FiltersState
import com.app.builder.ui.component.link.LinkPickerAction
import com.app.builder.ui.component.link.LinkPickerState
import com.app.builder.ui.component.link.toLinkItem
import com.app.builder.ui.component.list.EquipmentItem
import com.app.builder.ui.component.list.MaterialItem
import com.app.builder.ui.component.list.UserItem
import com.app.builder.ui.component.material.MaterialLinkPicker
import com.app.builder.ui.component.navigation.Navigation
import com.app.builder.ui.component.navigation.NavigationRoute
import com.app.builder.ui.component.session.SessionState
import com.app.builder.ui.component.user.UserLinkPicker
import com.app.builder.ui.devicelocation.LocalDeviceLocationProvider
import com.app.builder.ui.navigation.LocalRouter
import com.app.builder.ui.navigation.Router
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.permission.LocalPermissionManager
import com.app.builder.ui.permission.Permission
import com.app.builder.ui.screen.Screen
import com.app.builder.ui.store.Store
import com.app.builder.ui.store.previewStore

/**
 * The user-facing Building Detail Screen, showing a single building.
 *
 * @param sessionStore The session store shared by every screen.
 * @param filtersStore The building filters shared by every building screen, shown when the building list is beside this screen.
 * @param equipmentFiltersStore The equipment list filters, shown in the equipment picker.
 * @param equipmentPickerStore Provides the equipment picker state and receives its actions.
 * @param materialFiltersStore The material list filters, shown in the material picker.
 * @param materialPickerStore Provides the material picker state and receives its actions.
 * @param userFiltersStore The user list filters, shown in the user picker.
 * @param userPickerStore Provides the user picker state and receives its actions.
 * @param store Provides the building detail state and receives its actions.
 */
@Composable
fun BuildingDetailScreen(
    sessionStore: Store<SessionState, Unit>,
    filtersStore: Store<FiltersState, FiltersAction>,
    equipmentFiltersStore: Store<FiltersState, FiltersAction>,
    equipmentPickerStore: Store<LinkPickerState<Equipment, EquipmentItem>, LinkPickerAction>,
    materialFiltersStore: Store<FiltersState, FiltersAction>,
    materialPickerStore: Store<LinkPickerState<Material, MaterialItem>, LinkPickerAction>,
    userFiltersStore: Store<FiltersState, FiltersAction>,
    userPickerStore: Store<LinkPickerState<User, UserItem>, LinkPickerAction>,
    store: Store<BuildingDetailScreenState, BuildingDetailScreenAction>,
) {
    val state by store.stateFlow.collectAsStateWithLifecycle()
    val building = state.building
    val deleted = building?.deletedAt != null

    val router = LocalRouter.current
    val splitScreen = LocalSplitScreen.current
    val permissionManager = LocalPermissionManager.current
    val deviceLocationProvider = LocalDeviceLocationProvider.current
    val coroutineScope = rememberCoroutineScope()

    Screen(
        onBackClick = { router.back() },
        topBar = {
            ActionBar(
                sessionStore = sessionStore,
                filtersStore = filtersStore.takeIf { splitScreen },
                entityType = EntityType.BUILDING,
                editEntityTypes = setOfNotNull(EntityType.BUILDING_EQUIPMENT.takeIf { state.equipmentLinks.canEdit }, EntityType.BUILDING_MATERIAL.takeIf { state.materialLinks.canEdit }, EntityType.BUILDING_USER.takeIf { state.userLinks.canEdit }),
                deleted = deleted,
                onAddClick = if (splitScreen && !state.editing) {
                    { router.navigate(screen = Screen.BuildingDetail(uuid = uuid().toString(), new = true), option = Router.NavOption.REPLACE_LAST) }
                } else null,
                onEditClick = if (building != null && !state.editing && !deleted) {
                    { store.send(action = BuildingDetailScreenAction.Edit) }
                } else null,
                onSaveClick = if (state.editing) {
                    { store.send(action = BuildingDetailScreenAction.Save) }
                } else null,
                onCancelClick = if (state.editing) {
                    { store.send(action = BuildingDetailScreenAction.Cancel) }
                } else null,
                onDeleteClick = if (building != null && !state.editing) {
                    { store.send(action = if (it) BuildingDetailScreenAction.Delete else BuildingDetailScreenAction.Restore) }
                } else null,
            )
        },
        bottomBar = { Navigation(sessionStore = sessionStore, selected = NavigationRoute.BUILDING) },
    ) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(state = rememberScrollState())) {
            BuildingCard(
                enabled = building != null && state.editing && state.writable,
                uuid = building?.uuid?.toString(),
                modifiedAt = building?.modifiedAt?.toString(),
                deletedAt = building?.deletedAt?.toString(),
                name = building?.name,
                onNameChange = { store.send(action = BuildingDetailScreenAction.ChangeName(name = it)) },
                description = building?.description,
                onDescriptionChange = { store.send(action = BuildingDetailScreenAction.ChangeDescription(description = it)) },
                address = building?.address,
                onAddressChange = { store.send(action = BuildingDetailScreenAction.ChangeAddress(address = it)) },
                latitude = state.latitude.takeIf { building != null },
                onLatitudeChange = { store.send(action = BuildingDetailScreenAction.ChangeLatitude(latitude = it)) },
                latitudeError = state.latitudeError,
                longitude = state.longitude.takeIf { building != null },
                onLongitudeChange = { store.send(action = BuildingDetailScreenAction.ChangeLongitude(longitude = it)) },
                longitudeError = state.longitudeError,
                onUseLocationClick = if (deviceLocationProvider.available) {
                    {
                        coroutineScope.launch {
                            if (!permissionManager.grantPermission(permission = Permission.LOCATION)) return@launch
                            val location = deviceLocationProvider.getLastKnownLocation() ?: return@launch
                            store.send(action = BuildingDetailScreenAction.ChangeCoordinates(latitude = location.latitude, longitude = location.longitude))
                        }
                    }
                } else null,
                links = {
                    if (building != null && state.equipmentLinks.canRead) LinkSection(
                        tag = "building_equipments",
                        label = "building_equipments",
                        items = state.equipments.map { it.toLinkItem() }.toImmutableList(),
                        editing = state.editing && state.equipmentLinks.canEdit,
                        linkLabel = "building_link_equipments",
                        onLinkClick = { equipmentPickerStore.send(action = LinkPickerAction.Open(picked = state.equipments.map { it.uuid }.toSet())) },
                        onRemove = { item -> item.uuid.toUuid()?.let { store.send(action = BuildingDetailScreenAction.RemoveEquipment(equipmentUuid = it)) } },
                        onClick = { item -> router.navigate(screen = Screen.EquipmentDetail(uuid = item.uuid), option = Router.NavOption.ADD) }
                    )
                    if (building != null && state.materialLinks.canRead) LinkSection(
                        tag = "building_materials",
                        label = "building_materials",
                        items = state.materials.map { it.toLinkItem() }.toImmutableList(),
                        editing = state.editing && state.materialLinks.canEdit,
                        linkLabel = "building_link_materials",
                        onLinkClick = { materialPickerStore.send(action = LinkPickerAction.Open(picked = state.materials.map { it.uuid }.toSet())) },
                        onRemove = { item -> item.uuid.toUuid()?.let { store.send(action = BuildingDetailScreenAction.RemoveMaterial(materialUuid = it)) } },
                        onExtraChange = { item, extra ->
                            when (extra) {
                                is LinkExtra.Number -> item.uuid.toUuid()?.let { store.send(action = BuildingDetailScreenAction.ChangeMaterialQuantity(materialUuid = it, quantity = extra.value)) }
                            }
                        },
                        onClick = { item -> router.navigate(screen = Screen.MaterialDetail(uuid = item.uuid), option = Router.NavOption.ADD) }
                    )
                    if (building != null && state.userLinks.canRead) LinkSection(
                        tag = "building_users",
                        label = "building_users",
                        items = state.users.map { it.toLinkItem() }.toImmutableList(),
                        editing = state.editing && state.userLinks.canEdit,
                        linkLabel = "building_link_users",
                        onLinkClick = { userPickerStore.send(action = LinkPickerAction.Open(picked = state.users.map { it.uuid }.toSet())) },
                        onRemove = { item -> item.uuid.toUuid()?.let { store.send(action = BuildingDetailScreenAction.RemoveUser(userUuid = it)) } },
                        onClick = { item -> router.navigate(screen = Screen.UserDetail(uuid = item.uuid), option = Router.NavOption.ADD) }
                    )
                },
            )
        }
    }

    EquipmentLinkPicker(
        store = equipmentPickerStore,
        filtersStore = equipmentFiltersStore,
        title = "building_link_equipments",
        onConfirm = { store.send(action = BuildingDetailScreenAction.SetEquipments(equipments = it)) }
    )

    MaterialLinkPicker(
        store = materialPickerStore,
        filtersStore = materialFiltersStore,
        title = "building_link_materials",
        onConfirm = { store.send(action = BuildingDetailScreenAction.SetMaterials(materials = it)) }
    )

    UserLinkPicker(
        store = userPickerStore,
        filtersStore = userFiltersStore,
        title = "building_link_users",
        onConfirm = { store.send(action = BuildingDetailScreenAction.SetUsers(users = it)) }
    )
}

@PreviewLightDark
@Composable
private fun BuildingDetailScreenPreview() = Preview {
    InjectTranslations(
        translations = mapOf(
            "building_name" to "Name"
        )
    )
    val building = Building(
        name = "Big Compressor",
        description = "A description lalala",
        address = "Avenida dos Aliados, Porto",
        latitude = 41.1579,
        longitude = -8.6291
    )
    BuildingDetailScreen(
        sessionStore = previewStore(initialState = SessionState()),
        filtersStore = previewStore(initialState = FiltersState()),
        equipmentFiltersStore = previewStore(initialState = FiltersState()),
        equipmentPickerStore = previewStore(initialState = LinkPickerState()),
        materialFiltersStore = previewStore(initialState = FiltersState()),
        materialPickerStore = previewStore(initialState = LinkPickerState()),
        userFiltersStore = previewStore(initialState = FiltersState()),
        userPickerStore = previewStore(initialState = LinkPickerState()),
        store = previewStore(
            initialState = BuildingDetailScreenState(
                building = building,
                latitude = building.latitude.toString(),
                longitude = building.longitude.toString()
            )
        )
    )
}
