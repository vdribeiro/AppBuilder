package com.app.builder.ui.navigation.equipment

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
import com.app.builder.domain.Equipment
import com.app.builder.domain.Equipment.Property
import com.app.builder.domain.gateway.equipment.EquipmentUseCases
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
            is EquipmentListScreenAction.SelectEquipment -> selectEquipment(action = action)
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
        val selectedUuidFlow = snapshotFlow { (router.backStack.lastOrNull { it is Screen.EquipmentDetail } as? Screen.EquipmentDetail)?.uuid }
            .distinctUntilChanged()

        val equipmentsActionBarFlow = combine(
            flow = equipmentsFlow,
            flow2 = actionBarDataFlow,
        ) { equipments, actionBarData ->
            equipments to actionBarData
        }.mapLatest { (equipments, actionBarData) ->
            val comparator = equipmentComparator(sortProperty = actionBarData.sortProperty, sortAscending = actionBarData.sortAscending)
            equipments
                .filter { actionBarData.showDeleted || it.deletedAt == null }
                .filter { it.matchesSearch(search = actionBarData.search, searchableProperties = actionBarData.searchableProperties) }
                .let { if (comparator == null) it else it.sortedWith(comparator = comparator) } to actionBarData
        }

        combine(
            flow = equipmentsActionBarFlow,
            flow2 = selectedUuidFlow
        ) { (equipments, actionBar), selectedUuid ->
            equipments
                .map { it.toEquipmentItem(visibilityProperties = actionBar.visibleProperties, selectedUuid = selectedUuid) }
                .toPersistentList()
        }
            .flowOn(context = Dispatcher.Default)
            .observe(id = "filterEquipments") { equipments ->
                updateState { it.copy(equipments = equipments) }
            }

        Telemetry.info(tag = TAG, message = "Setup complete")
    }

    /**
     * Navigates to the detail screen of the selected equipment.
     *
     * @param action Action carrying the UUID of the selected equipment.
     * @return The [Job] representing this execution.
     */
    private fun selectEquipment(action: EquipmentListScreenAction.SelectEquipment): Job = launch(id = "selectEquipment") {
        router.navigate(screen = Screen.EquipmentDetail(uuid = action.equipmentUuid), option = Router.NavOption.REPLACE_LAST)
    }

    /**
     * Checks whether this equipment's searchable properties contain the given [search] text.
     *
     * @param search Search text to look for; an empty or blank value always matches.
     * @param searchableProperties Names of the [Property] values to search within.
     * @return `true` if any of the searchable properties contains [search], ignoring case.
     */
    private fun Equipment.matchesSearch(search: String, searchableProperties: List<String>): Boolean {
        return search.isBlank() || searchableProperties.any { property ->
            when (property) {
                Property.UUID.name -> uuid.toString()
                Property.NAME.name -> name
                Property.DESCRIPTION.name -> description
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
            Property.UUID.name -> compareBy<Equipment> { it.uuid.toString() }
            Property.MODIFIED_AT.name -> compareBy { it.modifiedAt }
            Property.DELETED_AT.name -> compareBy { it.deletedAt }
            Property.NAME.name -> compareBy { it.name }
            Property.DESCRIPTION.name -> compareBy { it.description }
            else -> return null
        }
        return if (sortAscending) comparator else comparator.reversed()
    }

    /**
     * Converts this equipment into an [EquipmentItem], including only the properties named in [visibilityProperties].
     *
     * @param visibilityProperties Names of the [Property] values that should be visible on the item.
     * @param selectedUuid UUID of the selected equipment.
     * @return Equipment item with hidden properties set to `null`.
     */
    private fun Equipment.toEquipmentItem(visibilityProperties: List<String>, selectedUuid: String?): EquipmentItem = EquipmentItem(
        uuid = uuid.toString(),
        selected = uuid.toString() == selectedUuid,
        deleted = deletedAt != null,
        uuidVisible = Property.UUID.name in visibilityProperties,
        modifiedAt = modifiedAt.takeIf { Property.MODIFIED_AT.name in visibilityProperties }?.let { getLocalDateTime(utc = it.toString()) },
        deletedAt = deletedAt.takeIf { Property.DELETED_AT.name in visibilityProperties }?.let { getLocalDateTime(utc = it.toString()) },
        name = name.takeIf { Property.NAME.name in visibilityProperties },
        description = description.takeIf { Property.DESCRIPTION.name in visibilityProperties },
        malfunctioning = (status == Equipment.Status.MALFUNCTIONING).takeIf { Property.STATUS.name in visibilityProperties }
    )

    companion object {
        private const val TAG = "EquipmentListScreenStore"
    }
}