package com.app.builder.ui.navigation.building

import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.mapNotNull
import androidx.compose.runtime.snapshotFlow
import com.app.builder.core.locale.getLocalDateTime
import com.app.builder.core.flow.Dispatcher
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.storage.AppFile
import com.app.builder.domain.Building
import com.app.builder.domain.Building.Property
import com.app.builder.domain.gateway.building.BuildingUseCases
import com.app.builder.ui.component.list.BuildingItem
import com.app.builder.ui.navigation.Router
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.store.Store

/**
 * Store backing the building list, combining buildings with the shared action bar state to filter, sort, and select them.
 *
 * @param state Initial building list state.
 * @property router The router used for navigation.
 * @property buildingUseCases Use cases used to observe and delete buildings.
 */
class BuildingListScreenStore(
    state: BuildingListScreenState,
    private val router: Router,
    private val buildingUseCases: BuildingUseCases
): Store<BuildingListScreenState, BuildingListScreenAction>(initialState = state) {
    init {
        setup()
    }

    override fun reducer(state: BuildingListScreenState, action: BuildingListScreenAction) {
        super.reducer(state = state, action = action)
        when (action) {
            is BuildingListScreenAction.SelectBuilding -> selectBuilding(action = action)
        }
    }

    /**
     * Observes buildings and keeps the displayed list in sync.
     *
     * @return The [Job] representing this execution.
     */
    private fun setup(): Job = launch(id = "setup") {
        Telemetry.info(tag = TAG, message = "Setup")

        val buildingsFlow = buildingUseCases.observeBuildings().toStateFlow(initialValue = emptyList())
        val actionBarDataFlow = AppFile.BuildingPreferences.cache()
            .mapNotNull { it }
            .distinctUntilChanged()
        val selectedUuidFlow = snapshotFlow { (router.backStack.lastOrNull { it is Screen.BuildingDetail } as? Screen.BuildingDetail)?.uuid }
            .distinctUntilChanged()

        val buildingsActionBarFlow = combine(
            flow = buildingsFlow,
            flow2 = actionBarDataFlow,
        ) { buildings, actionBarData ->
            buildings to actionBarData
        }.mapLatest { (buildings, actionBarData) ->
            val comparator = buildingComparator(sortProperty = actionBarData.sortProperty, sortAscending = actionBarData.sortAscending)
            buildings
                .filter { actionBarData.showDeleted || it.deletedAt == null }
                .filter { it.matchesSearch(search = actionBarData.search, searchableProperties = actionBarData.searchableProperties) }
                .let { if (comparator == null) it else it.sortedWith(comparator = comparator) } to actionBarData
        }

        combine(
            flow = buildingsActionBarFlow,
            flow2 = selectedUuidFlow
        ) { (buildings, actionBar), selectedUuid ->
            buildings
                .map { it.toBuildingItem(visibilityProperties = actionBar.visibleProperties, selectedUuid = selectedUuid) }
                .toPersistentList()
        }
            .flowOn(context = Dispatcher.Default)
            .observe(id = "filterBuildings") { buildings ->
                updateState { it.copy(buildings = buildings) }
            }

        Telemetry.info(tag = TAG, message = "Setup complete")
    }

    /**
     * Navigates to the detail screen of the selected building.
     *
     * @param action Action carrying the UUID of the selected building.
     * @return The [Job] representing this execution.
     */
    private fun selectBuilding(action: BuildingListScreenAction.SelectBuilding): Job = launch(id = "selectBuilding") {
        router.navigate(screen = Screen.BuildingDetail(uuid = action.buildingUuid), option = Router.NavOption.REPLACE_LAST)
    }

    /**
     * Checks whether this building's searchable properties contain the given [search] text.
     *
     * @param search Search text to look for; an empty or blank value always matches.
     * @param searchableProperties Names of the [Property] values to search within.
     * @return `true` if any of the searchable properties contains [search], ignoring case.
     */
    private fun Building.matchesSearch(search: String, searchableProperties: List<String>): Boolean {
        return search.isBlank() || searchableProperties.any { property ->
            when (property) {
                Property.UUID.name -> uuid.toString()
                Property.NAME.name -> name
                Property.DESCRIPTION.name -> description
                Property.ADDRESS.name -> address
                Property.MODIFIED_AT.name -> modifiedAt.toString()
                Property.DELETED_AT.name -> deletedAt?.toString()
                else -> null
            }?.contains(other = search, ignoreCase = true) == true
        }
    }

    /**
     * Builds a comparator for the given [sortProperty], reversed when [sortAscending] is `false`.
     *
     * @param sortProperty Name of the [Building.Property] to sort by.
     * @param sortAscending Whether the comparator should sort in ascending order.
     * @return Comparator for the given property, or `null` if it doesn't match a known [Building.Property].
     */
    private fun buildingComparator(sortProperty: String, sortAscending: Boolean): Comparator<Building>? {
        val comparator = when (sortProperty) {
            Property.UUID.name -> compareBy<Building> { it.uuid.toString() }
            Property.MODIFIED_AT.name -> compareBy { it.modifiedAt }
            Property.DELETED_AT.name -> compareBy { it.deletedAt }
            Property.NAME.name -> compareBy { it.name }
            Property.DESCRIPTION.name -> compareBy { it.description }
            Property.ADDRESS.name -> compareBy { it.address }
            Property.LATITUDE.name -> compareBy { it.latitude }
            Property.LONGITUDE.name -> compareBy { it.longitude }
            else -> return null
        }
        return if (sortAscending) comparator else comparator.reversed()
    }

    /**
     * Converts this building into a [BuildingItem], including only the properties named in [visibilityProperties].
     *
     * @param visibilityProperties Names of the [Property] values that should be visible on the item.
     * @param selectedUuid UUID of the selected building.
     * @return Building item with hidden properties set to `null`.
     */
    private fun Building.toBuildingItem(visibilityProperties: List<String>, selectedUuid: String?): BuildingItem = BuildingItem(
        uuid = uuid.toString(),
        selected = uuid.toString() == selectedUuid,
        deleted = deletedAt != null,
        uuidVisible = Property.UUID.name in visibilityProperties,
        modifiedAt = modifiedAt.takeIf { Property.MODIFIED_AT.name in visibilityProperties }?.let { getLocalDateTime(utc = it.toString()) },
        deletedAt = deletedAt.takeIf { Property.DELETED_AT.name in visibilityProperties }?.let { getLocalDateTime(utc = it.toString()) },
        name = name.takeIf { Property.NAME.name in visibilityProperties },
        description = description.takeIf { Property.DESCRIPTION.name in visibilityProperties },
        address = address.takeIf { Property.ADDRESS.name in visibilityProperties }
    )

    companion object {
        private const val TAG = "BuildingListScreenStore"
    }
}