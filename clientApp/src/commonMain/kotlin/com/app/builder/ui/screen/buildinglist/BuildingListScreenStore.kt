package com.app.builder.ui.screen.buildinglist

import kotlin.uuid.Uuid
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.mapNotNull
import com.app.builder.core.flow.Dispatcher
import com.app.builder.core.locale.now
import com.app.builder.core.security.toUuid
import com.app.builder.core.security.uuid
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.storage.AppFile
import com.app.builder.domain.Building
import com.app.builder.domain.Building.Property
import com.app.builder.domain.gateway.building.BuildingUseCases
import com.app.builder.plusOrMinus
import com.app.builder.toEnumOrNull
import com.app.builder.ui.component.bar.ActionBarMode
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
            is BuildingListScreenAction.SelectBuilding -> selectBuilding(state = state, action = action)
            is BuildingListScreenAction.Ok -> ok(state = state, action = action)
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
        val criteriaFlow = stateFlow
            .map { it.toBuildingListFilterCriteria() }
            .distinctUntilChanged()

        actionBarDataFlow
            .map { it.mode }
            .distinctUntilChanged()
            .observe(id = "syncMode") { newMode ->
                val mode = newMode.toEnumOrNull<ActionBarMode>() ?: return@observe

                if (mode == ActionBarMode.ADD) router.navigate(screen = Screen.BuildingDetail(uuid = uuid().toString()), option = Router.NavOption.REPLACE_LAST)

                updateState { state ->
                    state.copy(
                        mode = mode,
                        selectedUuids = when {
                            state.mode != mode -> persistentListOf()
                            else -> state.selectedUuids
                        }
                    )
                }
            }

        val buildingsActionBarFlow = combine(
            flow = buildingsFlow,
            flow2 = actionBarDataFlow,
        ) { buildings, actionBarData ->
            buildings to actionBarData
        }.mapLatest { (buildings, actionBarData) ->
            val comparator = buildingComparator(sortProperty = actionBarData.sortProperty, sortAscending = actionBarData.sortAscending)
            buildings
                .filter { it.matchesSearch(search = actionBarData.search, searchableProperties = actionBarData.searchableProperties) }
                .let { if (comparator == null) it else it.sortedWith(comparator = comparator) } to actionBarData
        }

        combine(
            flow = buildingsActionBarFlow,
            flow2 = criteriaFlow
        ) { (buildings, actionBar), criteria ->
            buildings.toPersistentList() to buildings
                .map { it.toBuildingItem(visibilityProperties = actionBar.visibleProperties, selectedUuids = criteria.selectedUuids) }
                .toPersistentList()
        }
            .flowOn(context = Dispatcher.Default)
            .observe(id = "filterBuildings") { (entities, buildings) ->
                updateState { it.copy(buildings = buildings, entities = entities) }
            }

        Telemetry.info(tag = TAG, message = "Setup complete")
    }

    /**
     * Toggles the selection of the tapped building when in delete mode, otherwise navigates to its detail screen.
     *
     * @param state Current building list state.
     * @param action Action carrying the UUID of the selected building.
     * @return The [Job] representing this execution.
     */
    private fun selectBuilding(state: BuildingListScreenState, action: BuildingListScreenAction.SelectBuilding): Job = launch(id = "selectBuilding") {
        when (state.mode) {
            ActionBarMode.DEFAULT,
            ActionBarMode.SEARCH,
            ActionBarMode.ADD,
            ActionBarMode.EDIT,
            ActionBarMode.DELETE -> router.navigate(screen = Screen.BuildingDetail(uuid = action.buildingUuid), option = Router.NavOption.REPLACE_LAST)

            ActionBarMode.BATCH_DELETE -> {
                val selectedUuid = action.buildingUuid.toUuid()
                if (selectedUuid != null) {
                    val selectedUuids = state.selectedUuids.plusOrMinus(element = selectedUuid).toPersistentList()
                    updateState { it.copy(selectedUuids = selectedUuids) }
                }
            }
        }
    }

    /**
     * Commits the pending mode once the user confirms it.
     *
     * @param state Current building list state.
     * @param action Action carrying the mode being confirmed.
     * @return The [Job] representing this execution.
     */
    private fun ok(state: BuildingListScreenState, action: BuildingListScreenAction.Ok): Job = launch(id = "ok") {
        when (action.mode) {
            ActionBarMode.DEFAULT,
            ActionBarMode.SEARCH,
            ActionBarMode.ADD,
            ActionBarMode.EDIT,
            ActionBarMode.DELETE -> Unit

            ActionBarMode.BATCH_DELETE -> {
                val now = now()
                state.entities.filter { it.uuid in state.selectedUuids }.forEach {
                    buildingUseCases.upsertBuilding(building = it.copy(modifiedAt = now, deletedAt = now))
                }
            }

        }
    }

    /**
     * Extracts the [BuildingListFilterCriteria] this state contributes to the displayed building list.
     *
     * @return The criteria derived from this state.
     */
    private fun BuildingListScreenState.toBuildingListFilterCriteria(): BuildingListFilterCriteria = BuildingListFilterCriteria(
        selectedUuids = selectedUuids
    )

    /**
     * Checks whether this building's searchable properties contain the given [search] text.
     *
     * @param search Search text to look for; an empty or blank value always matches.
     * @param searchableProperties Names of the [Property] values to search within.
     * @return `true` if any of the searchable properties contains [search], ignoring case.
     */
    private fun Building.matchesSearch(search: String, searchableProperties: List<String>): Boolean {
        if (search.isBlank()) return true
        return searchableProperties.any { property ->
            when (property) {
                Property.NAME.name -> name
                Property.CODE.name -> code
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
            Property.MODIFIED_AT.name -> compareBy<Building> { it.modifiedAt }
            Property.DELETED_AT.name -> compareBy { it.deletedAt }
            Property.NAME.name -> compareBy { it.name }
            Property.CODE.name -> compareBy { it.code }
            else -> return null
        }
        return if (sortAscending) comparator else comparator.reversed()
    }

    /**
     * Converts this building into a [BuildingItem], including only the properties named in [visibilityProperties].
     *
     * @param visibilityProperties Names of the [Property] values that should be visible on the item.
     * @param selectedUuids UUIDs of the buildings the user has picked.
     * @return Building item with hidden properties set to `null`.
     */
    private fun Building.toBuildingItem(visibilityProperties: List<String>, selectedUuids: ImmutableList<Uuid>): BuildingItem = BuildingItem(
        uuid = uuid.toString(),
        selected = uuid in selectedUuids,
        modifiedAt = modifiedAt.takeIf { Property.MODIFIED_AT.name in visibilityProperties }?.toString(),
        deletedAt = deletedAt.takeIf { Property.DELETED_AT.name in visibilityProperties }?.toString(),
        name = name.takeIf { Property.NAME.name in visibilityProperties },
        code = code.takeIf { Property.CODE.name in visibilityProperties }
    )

    companion object {
        private const val TAG = "BuildingListScreenStore"
    }
}
