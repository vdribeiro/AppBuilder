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
import com.app.builder.core.config.ClientFlags
import com.app.builder.core.locale.getLocalDateTime
import com.app.builder.core.security.toUuid
import com.app.builder.core.security.uuid
import com.app.builder.domain.Building
import com.app.builder.domain.EntityType
import com.app.builder.ui.InjectTranslations
import com.app.builder.ui.LocalSplitScreen
import com.app.builder.ui.Preview
import com.app.builder.ui.component.actionbar.ActionBar
import com.app.builder.ui.component.card.BuildingCard
import com.app.builder.ui.component.field.LinkItem
import com.app.builder.ui.component.field.LinkSection
import com.app.builder.ui.component.filters.FiltersAction
import com.app.builder.ui.component.filters.FiltersState
import com.app.builder.ui.component.filters.StoreFilters
import com.app.builder.ui.component.filters.StoreFiltersButton
import com.app.builder.ui.component.list.EquipmentList
import com.app.builder.ui.component.navigation.Navigation
import com.app.builder.ui.component.navigation.NavigationRoute
import com.app.builder.ui.component.overlay.PickerDialog
import com.app.builder.ui.component.session.SessionState
import com.app.builder.ui.devicelocation.LocalDeviceLocationProvider
import com.app.builder.ui.navigation.LocalRouter
import com.app.builder.ui.navigation.Router
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.permission.LocalPermissionManager
import com.app.builder.ui.permission.Permission
import com.app.builder.ui.screen.Screen
import com.app.builder.ui.store.Store
import com.app.builder.ui.store.previewStore
import com.app.builder.domain.Permission as DomainPermission

/**
 * The user-facing Building Detail Screen, showing a single building.
 *
 * @param sessionStore The session store shared by every screen.
 * @param filtersStore The building filters shared by every building screen, shown when the building list is beside this screen.
 * @param equipmentFiltersStore The equipment list filters, shown in the equipment picker.
 * @param store Provides the building detail state and receives its actions.
 */
@Composable
fun BuildingDetailScreen(
    sessionStore: Store<SessionState, Unit>,
    filtersStore: Store<FiltersState, FiltersAction>,
    equipmentFiltersStore: Store<FiltersState, FiltersAction>,
    store: Store<BuildingDetailScreenState, BuildingDetailScreenAction>,
) {
    val state by store.stateFlow.collectAsStateWithLifecycle()
    val session by sessionStore.stateFlow.collectAsStateWithLifecycle()
    val building = state.building
    val deleted = building?.deletedAt != null
    val account = session.account
    val canEditBuilding = account?.hasPermission(entityType = EntityType.BUILDING, permission = DomainPermission.WRITE) == true
    val canReadEquipments = account?.hasPermission(entityType = EntityType.EQUIPMENT, permission = DomainPermission.READ) == true
    val canReadLinks = ClientFlags.value.buildingEquipments && canReadEquipments && account.hasPermission(entityType = EntityType.BUILDING_EQUIPMENT, permission = DomainPermission.READ)
    val canEditLinks = ClientFlags.value.buildingEquipments && canReadEquipments && account.hasPermission(entityType = EntityType.BUILDING_EQUIPMENT, permission = DomainPermission.WRITE)

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
                editEntityTypes = if (canEditLinks) setOf(EntityType.BUILDING_EQUIPMENT) else emptySet(),
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
                enabled = building != null && state.editing && canEditBuilding,
                uuid = building?.uuid?.toString(),
                modifiedAt = building?.modifiedAt?.let { getLocalDateTime(utc = it.toString()) },
                deletedAt = building?.deletedAt?.let { getLocalDateTime(utc = it.toString()) },
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
                    if (building != null && canReadLinks) LinkSection(
                        tag = "building_equipments",
                        label = "building_equipments",
                        items = state.equipments.map { LinkItem(uuid = it.uuid.toString(), name = it.name) }.toImmutableList(),
                        editing = state.editing && canEditLinks,
                        linkLabel = "building_link_equipments",
                        onLinkClick = { store.send(action = BuildingDetailScreenAction.OpenEquipmentPicker) },
                        onRemove = { item -> item.uuid.toUuid()?.let { store.send(action = BuildingDetailScreenAction.RemoveEquipment(equipmentUuid = it)) } },
                        onClick = { item -> router.navigate(screen = Screen.EquipmentDetail(uuid = item.uuid), option = Router.NavOption.ADD) }
                    )
                },
            )
        }
    }

    if (state.equipmentPickerOpen) {
        val equipmentFilters by equipmentFiltersStore.stateFlow.collectAsStateWithLifecycle()
        PickerDialog(
            tag = "equipment_picker",
            title = "building_link_equipments",
            onSave = { store.send(action = BuildingDetailScreenAction.SaveEquipmentPick) },
            onDismiss = { store.send(action = BuildingDetailScreenAction.CloseEquipmentPicker) },
            actions = { StoreFiltersButton(filtersStore = equipmentFiltersStore) },
            secondaryActions = if (equipmentFilters.filtersExpanded) {
                { StoreFilters(filtersStore = equipmentFiltersStore, showDeletedToggle = false) }
            } else null,
        ) {
            EquipmentList(
                items = state.equipmentPickerItems,
                onClick = { item -> item.uuid.toUuid()?.let { store.send(action = BuildingDetailScreenAction.ToggleEquipmentPick(equipmentUuid = it)) } }
            )
        }
    }
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
        store = previewStore(
            initialState = BuildingDetailScreenState(
                building = building,
                latitude = building.latitude.toString(),
                longitude = building.longitude.toString()
            )
        )
    )
}
