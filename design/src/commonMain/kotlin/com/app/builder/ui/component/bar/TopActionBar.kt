package com.app.builder.ui.component.bar

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.ManageSearch
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SyncDisabled
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.app.builder.ui.LocalColorScheme
import com.app.builder.ui.Preview
import com.app.builder.ui.component.image.Avatar
import com.app.builder.ui.core.bar.TopBar
import com.app.builder.ui.core.bar.TopBarVariant
import com.app.builder.ui.core.button.Button
import com.app.builder.ui.core.button.Dropdown
import com.app.builder.ui.core.button.DropdownItem
import com.app.builder.ui.core.image.Icon
import com.app.builder.ui.core.image.Image
import com.app.builder.ui.core.text.Input
import com.app.builder.ui.core.text.Text

/**
 * Renders the top action bar, switching between the default, search and confirmation layouts based on the current mode.
 *
 * @param modifier The [Modifier] to be applied to the root layout.
 * @param title Text shown as the action bar's title.
 * @param properties Map of property keys to their display labels, used to populate the sort, visibility and search menus.
 * @param onBackClick Shows a back arrow when not null and executes this callback when clicked.
 * @param avatarName The name to fit in the avatar. Only the first letter of the first and last name will show capitalized if no image is provided.
 * @param avatarImage The image resource.
 * @param onAvatarClick Callback for the avatar click action.
 * @param synced Shows a sync disabled status icon in the error color after the avatar when `false`.
 * @param clockTrusted Shows a clock status icon in the error color after the avatar when `false`.
 * @param onAddClick Shows an add button when not null and executes this callback when clicked.
 * @param onEditClick Shows an edit button when not null and executes this callback when clicked.
 * @param onSaveClick Shows a save button when not null and executes this callback when clicked.
 * @param onCancelClick Shows a cancel button when not null and executes this callback when clicked.
 * @param onDeleteClick Shows a delete button when not null and executes this callback when clicked.
 * @param onLoginClick Shows a login button when not null and executes this callback when clicked.
 * @param onSearch Shows a search button when not null and is called with the debounced search query as the user types in search mode.
 * @param searchableProperties Property keys currently included when searching.
 * @param onSearchablePropertiesChange Shows a searchable properties selection button when not null and is called with the updated set of searchable property keys when one is toggled.
 * @param sortAscending Whether the list is currently sorted in ascending order.
 * @param onSortAscendingClick Shows a sort button when not null and is called with the new sort direction when the sort-direction button is clicked.
 * @param sortProperty Key of the property currently used to sort the list.
 * @param onSelectSortProperty Shows a sort properties selection button when not null and is called with the selected property key when a sort-property menu item is clicked.
 * @param visibleProperties Property keys currently shown as visible columns.
 * @param onVisiblePropertiesChange Shows a visible properties selection button when not null and is called with the updated set of visible property keys when one is toggled.
 */
@Composable
fun TopActionBar(
    modifier: Modifier = Modifier,
    title: String = "",
    properties: ImmutableMap<String, String> = persistentMapOf(),
    onBackClick: (() -> Unit)? = null,
    avatarName: String? = null,
    avatarImage: Image? = null,
    onAvatarClick: (() -> Unit)? = null,
    synced: Boolean = true,
    clockTrusted: Boolean = true,
    onAddClick: (() -> Unit)? = null,
    onEditClick: (() -> Unit)? = null,
    onSaveClick: (() -> Unit)? = null,
    onCancelClick: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null,
    onLoginClick: (() -> Unit)? = null,
    onSearch: ((String) -> Unit)? = null,
    searchableProperties: ImmutableList<String> = properties.keys.toPersistentList(),
    onSearchablePropertiesChange: ((String) -> Unit)? = null,
    sortAscending: Boolean = true,
    onSortAscendingClick: ((Boolean) -> Unit)? = null,
    sortProperty: String = "",
    onSelectSortProperty: ((String) -> Unit)? = null,
    visibleProperties: ImmutableList<String> = properties.keys.toPersistentList(),
    onVisiblePropertiesChange: ((String) -> Unit)? = null,
) {
    val color = LocalColorScheme.current

    var actionsExpanded by remember { mutableStateOf(value = false) }
    var searchMenuExpanded by remember { mutableStateOf(value = false) }
    var searchableMenuExpanded by remember { mutableStateOf(value = false) }
    var searchQuery by remember { mutableStateOf(value = "") }
    var sortingMenuExpanded by remember { mutableStateOf(value = false) }
    var visibilityMenuExpanded by remember { mutableStateOf(value = false) }

    onSearch?.let {
        LaunchedEffect(key1 = Unit) {
            snapshotFlow { searchQuery }
                .debounce(timeoutMillis = 300L)
                .distinctUntilChanged()
                .collect { onSearch(it) }
        }
    }

    val searchableProperties = onSearchablePropertiesChange?.let {
        properties.map { property ->
            DropdownItem(
                text = property.value,
                onClick = { onSearchablePropertiesChange(property.key) },
                leadingIcon = {
                    if (searchableProperties.contains(element = property.key)) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "checked"
                        )
                    } else Spacer(modifier = Modifier.size(size = 24.dp))
                }
            )
        }
    }.orEmpty().toPersistentList()

    val sortingProperties = onSelectSortProperty?.let {
        properties.map { property ->
            DropdownItem(
                text = property.value,
                onClick = { onSelectSortProperty(property.key) },
                leadingIcon = {
                    if (sortProperty == property.key) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "checked"
                        )
                    } else Spacer(modifier = Modifier.size(size = 24.dp))
                }
            )
        }
    }.orEmpty().toPersistentList()

    val visibilityProperties = onVisiblePropertiesChange?.let {
        properties.map { property ->
            DropdownItem(
                text = property.value,
                onClick = { onVisiblePropertiesChange(property.key) },
                leadingIcon = {
                    if (visibleProperties.contains(element = property.key)) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "checked"
                        )
                    } else Spacer(modifier = Modifier.size(size = 24.dp))
                }
            )
        }
    }.orEmpty().toPersistentList()

    TopBar(
        modifier = modifier.testTag(tag = "action_bar"),
        variant = TopBarVariant.SMALL,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
            ) {
                if (avatarName != null || avatarImage != null || onAvatarClick != null) Avatar(
                    modifier = Modifier
                        .testTag(tag = "action_avatar")
                        .clickable { onAvatarClick?.invoke() },
                    name = avatarName,
                    image = avatarImage,
                    size = 40.dp,
                )
                if (!synced) Icon(
                    modifier = Modifier.testTag(tag = "status_not_synced"),
                    imageVector = Icons.Default.SyncDisabled,
                    tint = color.error
                )
                if (!clockTrusted) Icon(
                    modifier = Modifier.testTag(tag = "status_clock_untrusted"),
                    imageVector = Icons.Default.Schedule,
                    tint = color.error
                )
                Text(text = title)
            }
        },
        navigationIcon = {
            if (onBackClick != null) Button(
                modifier = Modifier.testTag(tag = "action_back"),
                onClick = { onBackClick() },
                content = { Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack) }
            )
        },
        actions = {
            if (onAddClick != null) Button(
                modifier = Modifier.testTag(tag = "action_add"),
                onClick = { onAddClick() },
                content = { Icon(imageVector = Icons.Default.Add) }
            )

            if (onEditClick != null) Button(
                modifier = Modifier.testTag(tag = "action_edit"),
                onClick = { onEditClick() },
                content = { Icon(imageVector = Icons.Default.Edit) }
            )

            if (onSaveClick != null) Button(
                modifier = Modifier.testTag(tag = "action_save"),
                onClick = { onSaveClick() },
                content = { Icon(imageVector = Icons.Default.Save) }
            )

            if (onCancelClick != null) Button(
                modifier = Modifier.testTag(tag = "action_cancel"),
                onClick = { onCancelClick() },
                content = { Icon(imageVector = Icons.Default.Close) }
            )

            if (onDeleteClick != null) Button(
                modifier = Modifier.testTag(tag = "action_delete"),
                onClick = { onDeleteClick() },
                content = { Icon(imageVector = Icons.Default.Delete) }
            )

            if (onSearch != null || onSortAscendingClick != null || onSelectSortProperty != null || onVisiblePropertiesChange != null) Button(
                modifier = Modifier.testTag(tag = "action_expand"),
                onClick = { actionsExpanded = !actionsExpanded },
                content = { Icon(imageVector = if (actionsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore) }
            )

            if (onLoginClick != null) Button(
                modifier = Modifier.testTag(tag = "action_login"),
                onClick = { onLoginClick() },
                content = { Icon(imageVector = Icons.AutoMirrored.Filled.Login) }
            )
        },
        secondaryActions = if (!actionsExpanded) null else {
            {
                if (!searchMenuExpanded) {
                    if (onSearch != null) Button(
                        modifier = Modifier.testTag(tag = "action_search"),
                        onClick = {
                            searchQuery = ""
                            searchMenuExpanded = true
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
                            items = sortingProperties
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
                            items = visibilityProperties
                        )
                    }
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
                            items = searchableProperties
                        )
                    }
                    Button(
                        modifier = Modifier.testTag(tag = "action_search_close"),
                        onClick = {
                            searchMenuExpanded = false
                            searchQuery = ""
                        },
                        content = { Icon(imageVector = Icons.Default.Close) }
                    )
                }
            }
        }
    )
}

@PreviewLightDark
@Composable
private fun TopActionBarPreview() = Preview {
    TopActionBar(title = "None")
}

@PreviewLightDark
@Composable
private fun TopActionBarBackLoginPreview() = Preview {
    TopActionBar(
        title = "All",
        onBackClick = {},
        avatarName = "V R",
        onLoginClick = {},
    )
}

@PreviewLightDark
@Composable
private fun TopActionBarWritePreview() = Preview {
    TopActionBar(
        title = "All",
        properties = persistentMapOf(
            "name" to "Name",
            "type" to "Type",
        ),
        avatarName = "V R",
        synced = false,
        clockTrusted = false,
        onAddClick = {},
        onEditClick = {},
        onDeleteClick = {},
        onSearch = {},
        onSearchablePropertiesChange = {},
        onSortAscendingClick = {},
        sortProperty = "name",
        onSelectSortProperty = {},
        onVisiblePropertiesChange = {}
    )
}

@PreviewLightDark
@Composable
private fun TopActionBarEditPreview() = Preview {
    TopActionBar(
        properties = persistentMapOf(
            "name" to "Name",
            "type" to "Type",
        ),
        avatarName = "V R",
        synced = false,
        clockTrusted = false,
        onSaveClick = {},
        onCancelClick = {},
        onSearch = {},
        onSearchablePropertiesChange = {},
        onSortAscendingClick = {},
        sortProperty = "name",
        onSelectSortProperty = {},
        onVisiblePropertiesChange = {}
    )
}
