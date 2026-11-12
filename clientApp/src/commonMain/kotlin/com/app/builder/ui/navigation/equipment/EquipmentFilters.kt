package com.app.builder.ui.navigation.equipment

import kotlinx.collections.immutable.toImmutableMap
import androidx.compose.runtime.Composable
import com.app.builder.core.locale.getLocalDateTime
import com.app.builder.data.storage.AppFile
import com.app.builder.domain.Equipment
import com.app.builder.domain.Equipment.Property
import com.app.builder.ui.component.filters.FiltersStore
import com.app.builder.ui.component.list.EquipmentItem
import com.app.builder.ui.lifecycle.hostViewModel

/** The equipment properties by key, labelled by their translation keys. */
internal val equipmentProperties = Property.entries.associate { entry -> entry.name to entry.translationKey }.toImmutableMap()

/** The equipment properties shown by default. */
internal val equipmentVisibleProperties = listOf(
    Property.NAME,
    Property.DESCRIPTION,
    Property.STATUS,
).map { it.name }

/**
 * Gets the equipment filters store shared by the navigation host, creating it on first use.
 *
 * @return The equipment filters store.
 */
@Composable
internal fun equipmentFiltersStore(): FiltersStore = hostViewModel(key = FiltersStore.key(storageFile = AppFile.EquipmentPreferences)) {
    FiltersStore.get(
        properties = equipmentProperties,
        storageFile = AppFile.EquipmentPreferences,
        sortProperty = Property.MODIFIED_AT.name,
        sortAscending = false,
        visibleProperties = equipmentVisibleProperties
    )
}

/**
 * The default equipment filters, the same [equipmentFiltersStore] starts with.
 */
internal val equipmentFilterDefaults: AppFile.ActionBarData = AppFile.ActionBarData(
    searchableProperties = equipmentProperties.keys.toList(),
    sortAscending = false,
    sortProperty = Property.MODIFIED_AT.name,
    visibleProperties = equipmentVisibleProperties,
)

/**
 * Keeps the equipments matching [actionBarData]: leaves out the deleted ones unless they are shown, keeps those matching the search, and sorts them.
 *
 * @param actionBarData The equipment filters.
 * @return The matching equipments, sorted.
 */
internal fun List<Equipment>.filtered(actionBarData: AppFile.ActionBarData): List<Equipment> {
    val comparator = equipmentComparator(sortProperty = actionBarData.sortProperty, sortAscending = actionBarData.sortAscending)
    return filter { actionBarData.showDeleted || it.deletedAt == null }
        .filter { it.matchesSearch(search = actionBarData.search, searchableProperties = actionBarData.searchableProperties) }
        .let { if (comparator == null) it else it.sortedWith(comparator = comparator) }
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
internal fun Equipment.toEquipmentItem(visibilityProperties: List<String>, selectedUuid: String?): EquipmentItem = EquipmentItem(
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
