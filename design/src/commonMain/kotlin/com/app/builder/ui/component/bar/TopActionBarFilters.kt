package com.app.builder.ui.component.bar

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ManageSearch
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.app.builder.ui.Preview
import com.app.builder.ui.core.button.Button
import com.app.builder.ui.core.button.Dropdown
import com.app.builder.ui.core.button.DropdownItem
import com.app.builder.ui.core.image.Icon
import com.app.builder.ui.core.text.Input

/**
 * Renders the button expanding or collapsing the [Filters] for the [TopActionBar] actions slot.
 *
 * @param expanded Whether the filters are expanded.
 * @param onClick Called with the new expansion when clicked.
 */
@Composable
fun FiltersButton(expanded: Boolean, onClick: (Boolean) -> Unit) {
    Button(
        modifier = Modifier.testTag(tag = "action_expand"),
        onClick = { onClick(!expanded) },
        content = { Icon(imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore) }
    )
}

/**
 * Renders the search, sorting and property filters of a list for the [TopActionBar] secondary actions slot, switching between the filter buttons and the search input.
 *
 * @param properties Map of property keys to their display labels, used to populate the sort, visibility and search menus.
 * @param search The search query the search input starts with, and follows when it changes from outside.
 * @param searchExpanded Whether the search input is open.
 * @param onSearchExpandClick Called with the new value when the search input is opened or closed.
 * @param onSearch Shows a search button when not null and is called with the debounced search query as the user types in the search input.
 * @param searchableProperties Property keys currently included when searching.
 * @param onSearchablePropertiesChange Shows a searchable properties selection button when not null and is called with the key of the property toggled.
 * @param sortAscending Whether the list is currently sorted in ascending order.
 * @param onSortAscendingClick Shows a sort button when not null and is called with the new sort direction when clicked.
 * @param sortProperty Key of the property currently used to sort the list.
 * @param onSelectSortProperty Shows a sort properties selection button when not null and is called with the selected property key.
 * @param visibleProperties Property keys currently shown as visible columns.
 * @param onVisiblePropertiesChange Shows a visible properties selection button when not null and is called with the key of the property toggled.
 * @param showDeleted Whether deleted items are currently shown.
 * @param onShowDeletedClick Shows a deleted items toggle button when not null and is called with the new value when clicked.
 */
@Composable
fun Filters(
    properties: ImmutableMap<String, String> = persistentMapOf(),
    search: String = "",
    searchExpanded: Boolean = false,
    onSearchExpandClick: ((Boolean) -> Unit)? = null,
    onSearch: ((String) -> Unit)? = null,
    searchableProperties: ImmutableList<String> = properties.keys.toPersistentList(),
    onSearchablePropertiesChange: ((String) -> Unit)? = null,
    sortAscending: Boolean = true,
    onSortAscendingClick: ((Boolean) -> Unit)? = null,
    sortProperty: String = "",
    onSelectSortProperty: ((String) -> Unit)? = null,
    visibleProperties: ImmutableList<String> = properties.keys.toPersistentList(),
    onVisiblePropertiesChange: ((String) -> Unit)? = null,
    showDeleted: Boolean = false,
    onShowDeletedClick: ((Boolean) -> Unit)? = null,
) {
    var searchableMenuExpanded by remember { mutableStateOf(value = false) }
    var searchQuery by remember { mutableStateOf(value = search) }
    var sentSearch by remember { mutableStateOf(value = search) }
    var sortingMenuExpanded by remember { mutableStateOf(value = false) }
    var visibilityMenuExpanded by remember { mutableStateOf(value = false) }

    LaunchedEffect(key1 = search) {
        if (search != sentSearch) {
            sentSearch = search
            searchQuery = search
        }
    }
    onSearch?.let {
        LaunchedEffect(key1 = Unit) {
            snapshotFlow { searchQuery }
                .drop(count = 1)
                .debounce(timeoutMillis = 300L)
                .distinctUntilChanged()
                .collect {
                    sentSearch = it
                    onSearch(it)
                }
        }
    }

    if (!searchExpanded) {
        if (onSearch != null) Button(
            modifier = Modifier.testTag(tag = "action_search"),
            onClick = {
                searchQuery = ""
                onSearchExpandClick?.invoke(true)
            },
            content = { Icon(imageVector = Icons.Default.Search) }
        )
        if (onSortAscendingClick != null) Button(
            modifier = Modifier.testTag(tag = "action_sort"),
            onClick = { onSortAscendingClick(!sortAscending) },
            content = { Icon(imageVector = if (sortAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward) }
        )
        if (onSelectSortProperty != null) Box {
            Button(
                modifier = Modifier.testTag(tag = "action_sort_property"),
                onClick = { sortingMenuExpanded = !sortingMenuExpanded },
                content = { Icon(imageVector = Icons.Default.FilterList) }
            )
            Dropdown(
                modifier = Modifier.testTag(tag = "action_sort_property_menu"),
                expanded = sortingMenuExpanded,
                onDismissRequest = { sortingMenuExpanded = !sortingMenuExpanded },
                items = properties.toCheckedItems(checked = { it == sortProperty }, onClick = onSelectSortProperty)
            )
        }
        if (onVisiblePropertiesChange != null) Box {
            Button(
                modifier = Modifier.testTag(tag = "action_visible_properties"),
                onClick = { visibilityMenuExpanded = !visibilityMenuExpanded },
                content = { Icon(imageVector = Icons.Default.Visibility) }
            )
            Dropdown(
                modifier = Modifier.testTag(tag = "action_visible_properties_menu"),
                expanded = visibilityMenuExpanded,
                onDismissRequest = { visibilityMenuExpanded = !visibilityMenuExpanded },
                items = properties.toCheckedItems(checked = { it in visibleProperties }, onClick = onVisiblePropertiesChange)
            )
        }
        if (onShowDeletedClick != null) Button(
            modifier = Modifier.testTag(tag = "action_show_deleted"),
            checked = showDeleted,
            onClick = { onShowDeletedClick(it) },
            content = { Icon(imageVector = Icons.Outlined.Delete) }
        )
    } else {
        Input(
            modifier = Modifier
                .testTag(tag = "action_search_input")
                .padding(end = 4.dp),
            value = searchQuery,
            onValueChange = { searchQuery = it },
            maxLines = 1,
            leadingIcon = { Icon(imageVector = Icons.Default.Search) }
        )
        if (onSearchablePropertiesChange != null) Box {
            Button(
                modifier = Modifier.testTag(tag = "action_searchable_properties"),
                onClick = { searchableMenuExpanded = !searchableMenuExpanded },
                content = { Icon(imageVector = Icons.AutoMirrored.Filled.ManageSearch) }
            )
            Dropdown(
                modifier = Modifier.testTag(tag = "action_searchable_properties_menu"),
                expanded = searchableMenuExpanded,
                onDismissRequest = { searchableMenuExpanded = !searchableMenuExpanded },
                items = properties.toCheckedItems(checked = { it in searchableProperties }, onClick = onSearchablePropertiesChange)
            )
        }
        Button(
            modifier = Modifier.testTag(tag = "action_search_close"),
            onClick = {
                searchQuery = ""
                onSearchExpandClick?.invoke(false)
            },
            content = { Icon(imageVector = Icons.Default.Close) }
        )
    }
}

/**
 * Converts these properties into dropdown items, marking the checked ones.
 *
 * @param checked Whether the property with the given key is checked.
 * @param onClick Called with the key of the clicked property.
 * @return The dropdown items, in the order of these properties.
 */
private fun ImmutableMap<String, String>.toCheckedItems(checked: (String) -> Boolean, onClick: (String) -> Unit): ImmutableList<DropdownItem> = map { property ->
    DropdownItem(
        text = property.value,
        onClick = { onClick(property.key) },
        leadingIcon = {
            if (checked(property.key)) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "checked"
                )
            } else Spacer(modifier = Modifier.size(size = 24.dp))
        }
    )
}.toPersistentList()

@PreviewLightDark
@Composable
private fun FiltersButtonPreview() = Preview {
    Row {
        FiltersButton(expanded = false, onClick = {})
        FiltersButton(expanded = true, onClick = {})
    }
}

@PreviewLightDark
@Composable
private fun FiltersPreview() = Preview {
    Row {
        Filters(
            properties = persistentMapOf("name" to "Name", "type" to "Type"),
            onSearch = {},
            onSortAscendingClick = {},
            sortProperty = "name",
            onSelectSortProperty = {},
            onVisiblePropertiesChange = {},
            showDeleted = true
        ) {}
    }
}

@PreviewLightDark
@Composable
private fun FiltersSearchPreview() = Preview {
    Row {
        Filters(
            properties = persistentMapOf("name" to "Name", "type" to "Type"),
            search = "query",
            searchExpanded = true,
            onSearch = {},
            searchableProperties = persistentListOf("name"),
            onSearchablePropertiesChange = {},
        )
    }
}
