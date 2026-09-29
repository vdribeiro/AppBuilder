package com.app.builder.ui.screen.equipmentlist

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
import com.app.builder.domain.Equipment
import com.app.builder.domain.Equipment.Property
import com.app.builder.domain.gateway.equipment.EquipmentUseCases
import com.app.builder.plusOrMinus
import com.app.builder.toEnumOrNull
import com.app.builder.ui.component.bar.ActionBarMode
import com.app.builder.ui.component.list.EquipmentItem
import com.app.builder.ui.navigation.Router
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.store.Store

/**
 * Store backing the equipment list, combining equipments with the shared action bar state to filter, sort, and select them.
 *
 * @param state Initial equipment list state.
 * @property router The router used for navigation.
 * @property equipmentUseCases Use cases used to observe and delete equipments.
 */
class EquipmentListScreenStore(
    state: EquipmentListScreenState,
    private val router: Router,
    private val equipmentUseCases: EquipmentUseCases
): Store<EquipmentListScreenState, EquipmentListScreenAction>(initialState = state) {
    init {
        setup()
    }

    override fun reducer(state: EquipmentListScreenState, action: EquipmentListScreenAction) {
        super.reducer(state = state, action = action)
        when (action) {
            is EquipmentListScreenAction.SelectEquipment -> selectEquipment(state = state, action = action)
            is EquipmentListScreenAction.Ok -> ok(state = state, action = action)
        }
    }

    /**
     * Observes equipments and keeps the displayed list in sync.
     *
     * @return The [Job] representing this execution.
     */
    private fun setup(): Job = launch(id = "setup") {
        Telemetry.info(tag = TAG, message = "Setup")

        val equipmentsFlow = equipmentUseCases.observeEquipments().toStateFlow(initialValue = emptyList())
        val actionBarDataFlow = AppFile.EquipmentPreferences.cache()
            .mapNotNull { it }
            .distinctUntilChanged()
        val criteriaFlow = stateFlow
            .map { it.toEquipmentListFilterCriteria() }
            .distinctUntilChanged()

        actionBarDataFlow
            .map { it.mode }
            .distinctUntilChanged()
            .observe(id = "syncMode") { newMode ->
                val mode = newMode.toEnumOrNull<ActionBarMode>() ?: return@observe

                if (mode == ActionBarMode.ADD) router.navigate(screen = Screen.EquipmentDetail(uuid = uuid().toString()), option = Router.NavOption.REPLACE_LAST)

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

        val equipmentsActionBarFlow = combine(
            flow = equipmentsFlow,
            flow2 = actionBarDataFlow,
        ) { equipments, actionBarData ->
            equipments to actionBarData
        }.mapLatest { (equipments, actionBarData) ->
            val comparator = equipmentComparator(sortProperty = actionBarData.sortProperty, sortAscending = actionBarData.sortAscending)
            equipments
                .filter { it.matchesSearch(search = actionBarData.search, searchableProperties = actionBarData.searchableProperties) }
                .let { if (comparator == null) it else it.sortedWith(comparator = comparator) } to actionBarData
        }

        combine(
            flow = equipmentsActionBarFlow,
            flow2 = criteriaFlow
        ) { (equipments, actionBar), criteria ->
            equipments.toPersistentList() to equipments
                .map { it.toEquipmentItem(visibilityProperties = actionBar.visibleProperties, selectedUuids = criteria.selectedUuids) }
                .toPersistentList()
        }
            .flowOn(context = Dispatcher.Default)
            .observe(id = "filterEquipments") { (entities, equipments) ->
                updateState { it.copy(equipments = equipments, entities = entities) }
            }

        Telemetry.info(tag = TAG, message = "Setup complete")
    }

    /**
     * Toggles the selection of the tapped equipment when in delete mode, otherwise navigates to its detail screen.
     *
     * @param state Current equipment list state.
     * @param action Action carrying the UUID of the selected equipment.
     * @return The [Job] representing this execution.
     */
    private fun selectEquipment(state: EquipmentListScreenState, action: EquipmentListScreenAction.SelectEquipment): Job = launch(id = "selectEquipment") {
        when (state.mode) {
            ActionBarMode.DEFAULT,
            ActionBarMode.SEARCH,
            ActionBarMode.ADD,
            ActionBarMode.EDIT,
            ActionBarMode.DELETE -> router.navigate(screen = Screen.EquipmentDetail(uuid = action.equipmentUuid), option = Router.NavOption.REPLACE_LAST)

            ActionBarMode.BATCH_DELETE -> {
                val selectedUuid = action.equipmentUuid.toUuid()
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
     * @param state Current equipment list state.
     * @param action Action carrying the mode being confirmed.
     * @return The [Job] representing this execution.
     */
    private fun ok(state: EquipmentListScreenState, action: EquipmentListScreenAction.Ok): Job = launch(id = "ok") {
        when (action.mode) {
            ActionBarMode.DEFAULT,
            ActionBarMode.SEARCH,
            ActionBarMode.ADD,
            ActionBarMode.EDIT,
            ActionBarMode.DELETE -> Unit

            ActionBarMode.BATCH_DELETE -> {
                val now = now()
                state.entities.filter { it.uuid in state.selectedUuids }.forEach {
                    equipmentUseCases.upsertEquipment(equipment = it.copy(modifiedAt = now, deletedAt = now))
                }
            }

        }
    }

    /**
     * Extracts the [EquipmentListFilterCriteria] this state contributes to the displayed equipment list.
     *
     * @return The criteria derived from this state.
     */
    private fun EquipmentListScreenState.toEquipmentListFilterCriteria(): EquipmentListFilterCriteria = EquipmentListFilterCriteria(
        selectedUuids = selectedUuids
    )

    /**
     * Checks whether this equipment's searchable properties contain the given [search] text.
     *
     * @param search Search text to look for; an empty or blank value always matches.
     * @param searchableProperties Names of the [Property] values to search within.
     * @return `true` if any of the searchable properties contains [search], ignoring case.
     */
    private fun Equipment.matchesSearch(search: String, searchableProperties: List<String>): Boolean {
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
     * @param sortProperty Name of the [Equipment.Property] to sort by.
     * @param sortAscending Whether the comparator should sort in ascending order.
     * @return Comparator for the given property, or `null` if it doesn't match a known [Equipment.Property].
     */
    private fun equipmentComparator(sortProperty: String, sortAscending: Boolean): Comparator<Equipment>? {
        val comparator = when (sortProperty) {
            Property.MODIFIED_AT.name -> compareBy<Equipment> { it.modifiedAt }
            Property.DELETED_AT.name -> compareBy { it.deletedAt }
            Property.NAME.name -> compareBy { it.name }
            Property.CODE.name -> compareBy { it.code }
            else -> return null
        }
        return if (sortAscending) comparator else comparator.reversed()
    }

    /**
     * Converts this equipment into a [EquipmentItem], including only the properties named in [visibilityProperties].
     *
     * @param visibilityProperties Names of the [Property] values that should be visible on the item.
     * @param selectedUuids UUIDs of the equipments the user has picked.
     * @return Equipment item with hidden properties set to `null`.
     */
    private fun Equipment.toEquipmentItem(visibilityProperties: List<String>, selectedUuids: ImmutableList<Uuid>): EquipmentItem = EquipmentItem(
        uuid = uuid.toString(),
        selected = uuid in selectedUuids,
        modifiedAt = modifiedAt.takeIf { Property.MODIFIED_AT.name in visibilityProperties }?.toString(),
        deletedAt = deletedAt.takeIf { Property.DELETED_AT.name in visibilityProperties }?.toString(),
        name = name.takeIf { Property.NAME.name in visibilityProperties },
        code = code.takeIf { Property.CODE.name in visibilityProperties }
    )

    companion object {
        private const val TAG = "EquipmentListScreenStore"
    }
}
