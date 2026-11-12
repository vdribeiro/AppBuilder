package com.app.builder.ui.navigation.building

import kotlinx.collections.immutable.toImmutableMap
import androidx.compose.runtime.Composable
import com.app.builder.core.locale.getLocalDateTime
import com.app.builder.data.storage.AppFile
import com.app.builder.domain.Building
import com.app.builder.domain.Building.Property
import com.app.builder.ui.component.filters.FiltersStore
import com.app.builder.ui.component.list.BuildingItem
import com.app.builder.ui.lifecycle.hostViewModel

/** The building properties by key, labelled by their translation keys. */
internal val buildingProperties = Property.entries.associate { entry -> entry.name to entry.translationKey }.toImmutableMap()

/** The building properties shown by default. */
internal val buildingVisibleProperties = listOf(
    Property.NAME,
    Property.DESCRIPTION,
    Property.ADDRESS,
).map { it.name }

/**
 * Gets the building filters store shared by the navigation host, creating it on first use.
 *
 * @return The building filters store.
 */
@Composable
internal fun buildingFiltersStore(): FiltersStore = hostViewModel(key = FiltersStore.key(storageFile = AppFile.BuildingPreferences)) {
    FiltersStore.get(
        properties = buildingProperties,
        storageFile = AppFile.BuildingPreferences,
        sortProperty = Property.MODIFIED_AT.name,
        sortAscending = false,
        visibleProperties = buildingVisibleProperties
    )
}

/**
 * The default building filters, the same [buildingFiltersStore] starts with.
 */
internal val buildingFilterDefaults: AppFile.ActionBarData = AppFile.ActionBarData(
    searchableProperties = buildingProperties.keys.toList(),
    sortAscending = false,
    sortProperty = Property.MODIFIED_AT.name,
    visibleProperties = buildingVisibleProperties,
)

/**
 * Keeps the buildings matching [actionBarData]: leaves out the deleted ones unless they are shown, keeps those matching the search, and sorts them.
 *
 * @param actionBarData The building filters.
 * @return The matching buildings, sorted.
 */
internal fun List<Building>.filtered(actionBarData: AppFile.ActionBarData): List<Building> {
    val comparator = buildingComparator(sortProperty = actionBarData.sortProperty, sortAscending = actionBarData.sortAscending)
    return filter { actionBarData.showDeleted || it.deletedAt == null }
        .filter { it.matchesSearch(search = actionBarData.search, searchableProperties = actionBarData.searchableProperties) }
        .let { if (comparator == null) it else it.sortedWith(comparator = comparator) }
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
internal fun Building.toBuildingItem(visibilityProperties: List<String>, selectedUuid: String?): BuildingItem = BuildingItem(
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
