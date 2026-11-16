package com.app.builder.ui.navigation.material

import kotlinx.collections.immutable.toImmutableMap
import androidx.compose.runtime.Composable
import com.app.builder.core.locale.getLocalDateTime
import com.app.builder.data.storage.AppFile
import com.app.builder.domain.Material
import com.app.builder.domain.Material.Property
import com.app.builder.ui.component.filters.FiltersStore
import com.app.builder.ui.component.list.MaterialItem
import com.app.builder.ui.lifecycle.hostViewModel

/** The material properties by key, labelled by their translation keys. */
internal val materialProperties = Property.entries.associate { entry -> entry.name to entry.translationKey }.toImmutableMap()

/** The material properties shown by default. */
internal val materialVisibleProperties = listOf(
    Property.NAME,
    Property.DESCRIPTION,
).map { it.name }

/**
 * Gets the material filters store shared by the navigation host, creating it on first use.
 *
 * @return The material filters store.
 */
@Composable
internal fun materialFiltersStore(): FiltersStore = hostViewModel(key = FiltersStore.key(storageFile = AppFile.MaterialPreferences)) {
    FiltersStore.get(
        properties = materialProperties,
        storageFile = AppFile.MaterialPreferences,
        sortProperty = Property.MODIFIED_AT.name,
        sortAscending = false,
        visibleProperties = materialVisibleProperties
    )
}

/**
 * The default material filters, the same [materialFiltersStore] starts with.
 */
internal val materialFilterDefaults: AppFile.ActionBarData = AppFile.ActionBarData(
    searchableProperties = materialProperties.keys.toList(),
    sortAscending = false,
    sortProperty = Property.MODIFIED_AT.name,
    visibleProperties = materialVisibleProperties,
)

/**
 * Keeps the materials matching [actionBarData]: leaves out the deleted ones unless they are shown, keeps those matching the search, and sorts them.
 *
 * @param actionBarData The material filters.
 * @return The matching materials, sorted.
 */
internal fun List<Material>.filtered(actionBarData: AppFile.ActionBarData): List<Material> {
    val comparator = materialComparator(sortProperty = actionBarData.sortProperty, sortAscending = actionBarData.sortAscending)
    return filter { actionBarData.showDeleted || it.deletedAt == null }
        .filter { it.matchesSearch(search = actionBarData.search, searchableProperties = actionBarData.searchableProperties) }
        .let { if (comparator == null) it else it.sortedWith(comparator = comparator) }
}

/**
 * Checks whether this material's searchable properties contain the given [search] text.
 *
 * @param search Search text to look for; an empty or blank value always matches.
 * @param searchableProperties Names of the [Property] values to search within.
 * @return `true` if any of the searchable properties contains [search], ignoring case.
 */
private fun Material.matchesSearch(search: String, searchableProperties: List<String>): Boolean {
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
 * @param sortProperty Name of the [Material.Property] to sort by.
 * @param sortAscending Whether the comparator should sort in ascending order.
 * @return Comparator for the given property, or `null` if it doesn't match a known [Material.Property].
 */
private fun materialComparator(sortProperty: String, sortAscending: Boolean): Comparator<Material>? {
    val comparator = when (sortProperty) {
        Property.UUID.name -> compareBy<Material> { it.uuid.toString() }
        Property.MODIFIED_AT.name -> compareBy { it.modifiedAt }
        Property.DELETED_AT.name -> compareBy { it.deletedAt }
        Property.NAME.name -> compareBy { it.name }
        Property.DESCRIPTION.name -> compareBy { it.description }
        else -> return null
    }
    return if (sortAscending) comparator else comparator.reversed()
}

/**
 * Converts this material into an [MaterialItem], including only the properties named in [visibilityProperties].
 *
 * @param visibilityProperties Names of the [Property] values that should be visible on the item.
 * @param selectedUuid UUID of the selected material.
 * @return Material item with hidden properties set to `null`.
 */
internal fun Material.toMaterialItem(visibilityProperties: List<String>, selectedUuid: String?): MaterialItem = MaterialItem(
    uuid = uuid.toString(),
    selected = uuid.toString() == selectedUuid,
    deleted = deletedAt != null,
    uuidVisible = Property.UUID.name in visibilityProperties,
    modifiedAt = modifiedAt.takeIf { Property.MODIFIED_AT.name in visibilityProperties }?.let { getLocalDateTime(utc = it.toString()) },
    deletedAt = deletedAt.takeIf { Property.DELETED_AT.name in visibilityProperties }?.let { getLocalDateTime(utc = it.toString()) },
    name = name.takeIf { Property.NAME.name in visibilityProperties },
    description = description.takeIf { Property.DESCRIPTION.name in visibilityProperties }
)
