package com.app.builder.ui.component.user

import kotlinx.collections.immutable.toImmutableMap
import androidx.compose.runtime.Composable
import com.app.builder.data.storage.AppFile
import com.app.builder.domain.User
import com.app.builder.domain.User.Property
import com.app.builder.ui.component.filters.FiltersStore
import com.app.builder.ui.component.list.UserItem
import com.app.builder.ui.core.image.Image
import com.app.builder.ui.lifecycle.hostViewModel

/** The user properties by key, labelled by their translation keys. */
internal val userProperties = Property.entries.associate { entry -> entry.name to entry.translationKey }.toImmutableMap()

/** The user properties shown by default. */
internal val userVisibleProperties = listOf(
    Property.NAME,
).map { it.name }

/**
 * Gets the user filters store shared by the navigation host, creating it on first use.
 *
 * @return The user filters store.
 */
@Composable
internal fun userFiltersStore(): FiltersStore = hostViewModel(key = FiltersStore.key(storageFile = AppFile.UserPreferences)) {
    FiltersStore.get(
        properties = userProperties,
        storageFile = AppFile.UserPreferences,
        sortProperty = Property.NAME.name,
        sortAscending = true,
        visibleProperties = userVisibleProperties
    )
}

/**
 * The default user filters, the same [userFiltersStore] starts with.
 */
internal val userFilterDefaults: AppFile.ActionBarData = AppFile.ActionBarData(
    searchableProperties = userProperties.keys.toList(),
    sortAscending = true,
    sortProperty = Property.NAME.name,
    visibleProperties = userVisibleProperties,
)

/**
 * Keeps the users matching [actionBarData]: leaves out the deleted ones unless they are shown, keeps those matching the search, and sorts them.
 *
 * @param actionBarData The user filters.
 * @return The matching users, sorted.
 */
internal fun List<User>.filtered(actionBarData: AppFile.ActionBarData): List<User> {
    val comparator = userComparator(sortProperty = actionBarData.sortProperty, sortAscending = actionBarData.sortAscending)
    return filter { actionBarData.showDeleted || it.deletedAt == null }
        .filter { it.matchesSearch(search = actionBarData.search, searchableProperties = actionBarData.searchableProperties) }
        .let { if (comparator == null) it else it.sortedWith(comparator = comparator) }
}

/**
 * Checks whether this user's searchable properties contain the given [search] text.
 *
 * @param search Search text to look for; an empty or blank value always matches.
 * @param searchableProperties Names of the [Property] values to search within.
 * @return `true` if any of the searchable properties contains [search], ignoring case.
 */
private fun User.matchesSearch(search: String, searchableProperties: List<String>): Boolean {
    return search.isBlank() || searchableProperties.any { property ->
        when (property) {
            Property.UUID.name -> uuid.toString()
            Property.NAME.name -> name
            Property.MODIFIED_AT.name -> modifiedAt.toString()
            Property.DELETED_AT.name -> deletedAt?.toString()
            else -> null
        }?.contains(other = search, ignoreCase = true) == true
    }
}

/**
 * Builds a comparator for the given [sortProperty], reversed when [sortAscending] is `false`.
 *
 * @param sortProperty Name of the [User.Property] to sort by.
 * @param sortAscending Whether the comparator should sort in ascending order.
 * @return Comparator for the given property, or `null` if it doesn't match a known [User.Property].
 */
private fun userComparator(sortProperty: String, sortAscending: Boolean): Comparator<User>? {
    val comparator = when (sortProperty) {
        Property.UUID.name -> compareBy<User> { it.uuid.toString() }
        Property.MODIFIED_AT.name -> compareBy { it.modifiedAt }
        Property.DELETED_AT.name -> compareBy { it.deletedAt }
        Property.NAME.name -> compareBy { it.name }
        else -> return null
    }
    return if (sortAscending) comparator else comparator.reversed()
}

/**
 * Converts this user into a [UserItem], including only the properties named in [visibilityProperties].
 *
 * @param visibilityProperties Names of the [Property] values that should be visible on the item.
 * @param selectedUuid UUID of the selected user.
 * @return User item with hidden properties set to `null`.
 */
internal fun User.toUserItem(visibilityProperties: List<String>, selectedUuid: String?): UserItem = UserItem(
    uuid = uuid.toString(),
    selected = uuid.toString() == selectedUuid,
    deleted = deletedAt != null,
    uuidVisible = Property.UUID.name in visibilityProperties,
    modifiedAt = modifiedAt.takeIf { Property.MODIFIED_AT.name in visibilityProperties }?.toString(),
    deletedAt = deletedAt.takeIf { Property.DELETED_AT.name in visibilityProperties }?.toString(),
    name = name.takeIf { Property.NAME.name in visibilityProperties },
    avatar = avatar?.let { Image(url = it) }
)
