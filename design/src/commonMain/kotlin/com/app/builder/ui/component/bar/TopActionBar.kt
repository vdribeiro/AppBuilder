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
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
 * @param onBackClick Shows a back arrow when not null and executes this callback when clicked.
 * @param onLoginClick Shows a login button when not null and executes this callback when clicked.
 * @param avatarName The name to fit in the avatar. Only the first letter of the first and last name will show capitalized if no image is provided.
 * @param avatarImage The image resource.
 * @param onAvatarClick Callback for the avatar click action.
 * @param mode Current display mode, controlling which actions/inputs are shown.
 * @param layout Which set of actions to show, based on whether the screen displays a list, a detail, or both.
 * @param write Whether buttons that allow "write" operations should be shown.
 * @param onModeChange Called with the new mode whenever the user triggers a mode change.
 * @param onOkClick Called with the current [mode] when the confirmation button is clicked.
 * @param onCancelClick Called with the current [mode] when the cancellation button is clicked.
 * @param onSearch Called with the debounced search query as the user types in search mode.
 * @param sortAscending Whether the list is currently sorted in ascending order.
 * @param sortProperty Key of the property currently used to sort the list.
 * @param onSelectSortProperty Called with the selected property key when a sort-property menu item is clicked.
 * @param onSortAscendingClick Called with the new sort direction when the sort-direction button is clicked.
 * @param properties Map of property keys to their display labels, used to populate the sort, visibility and search menus.
 * @param visibleProperties Property keys currently shown as visible columns.
 * @param onVisiblePropertiesChange Called with the updated set of visible property keys when one is toggled.
 * @param searchableProperties Property keys currently included when searching.
 * @param onSearchablePropertiesChange Called with the updated set of searchable property keys when one is toggled.
 */
@Composable
fun TopActionBar(
    modifier: Modifier = Modifier,
    title: String = "",
    onBackClick: (() -> Unit)? = null,
    onLoginClick: (() -> Unit)? = null,
    avatarName: String? = null,
    avatarImage: Image? = null,
    onAvatarClick: (() -> Unit)? = null,
    mode: ActionBarMode = ActionBarMode.DEFAULT,
    layout: ActionBarLayout = ActionBarLayout.NONE,
    write: Boolean = false,
    onModeChange: (ActionBarMode) -> Unit = {},
    onOkClick: (ActionBarMode) -> Unit = {},
    onCancelClick: (ActionBarMode) -> Unit = {},
    onSearch: (String) -> Unit = {},
    sortAscending: Boolean = true,
    sortProperty: String = "",
    onSelectSortProperty: (String) -> Unit = {},
    onSortAscendingClick: (Boolean) -> Unit = {},
    properties: ImmutableMap<String, String> = persistentMapOf(),
    visibleProperties: ImmutableList<String> = properties.keys.toPersistentList(),
    onVisiblePropertiesChange: (String) -> Unit = {},
    searchableProperties: ImmutableList<String> = properties.keys.toPersistentList(),
    onSearchablePropertiesChange: (String) -> Unit = {},
) {
    var actionsExpanded by remember { mutableStateOf(value = false) }
    var sortingMenuExpanded by remember { mutableStateOf(value = false) }
    var visibilityMenuExpanded by remember { mutableStateOf(value = false) }
    var searchableMenuExpanded by remember { mutableStateOf(value = false) }
    var searchQuery by remember { mutableStateOf(value = "") }
    LaunchedEffect(key1 = Unit) {
        snapshotFlow { searchQuery }
            .debounce(timeoutMillis = 300L)
            .distinctUntilChanged()
            .collect { onSearch(it) }
    }

    val sortingProperties = properties.map { property ->
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
    }.toPersistentList()

    val visibilityProperties = properties.map { property ->
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
    }.toPersistentList()

    val searchableProperties = properties.map { property ->
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
    }.toPersistentList()

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
            when (mode) {
                ActionBarMode.DEFAULT -> {
                    if (write && (layout == ActionBarLayout.ALL || layout == ActionBarLayout.LIST)) {
                        Button(
                            modifier = Modifier.testTag(tag = "action_add"),
                            onClick = { onModeChange(ActionBarMode.ADD) },
                            content = { Icon(imageVector = Icons.Default.Add) }
                        )
                        Button(
                            modifier = Modifier.testTag(tag = "action_batch_delete"),
                            onClick = { onModeChange(ActionBarMode.BATCH_DELETE) },
                            content = { Icon(imageVector = Icons.Default.DeleteSweep) }
                        )
                    }

                    if (write && (layout == ActionBarLayout.ALL || layout == ActionBarLayout.DETAIL)) {
                        Button(
                            modifier = Modifier.testTag(tag = "action_edit"),
                            onClick = { onModeChange(ActionBarMode.EDIT) },
                            content = { Icon(imageVector = Icons.Default.Edit) }
                        )
                        Button(
                            modifier = Modifier.testTag(tag = "action_delete"),
                            onClick = { onModeChange(ActionBarMode.DELETE) },
                            content = { Icon(imageVector = Icons.Default.Delete) }
                        )
                    }

                    if (layout == ActionBarLayout.ALL || layout == ActionBarLayout.LIST) {
                        Button(
                            modifier = Modifier.testTag(tag = "action_expand"),
                            onClick = { actionsExpanded = !actionsExpanded },
                            content = { Icon(imageVector = if (actionsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore) }
                        )
                    }

                    if (onLoginClick != null) Button(
                        modifier = Modifier.testTag(tag = "action_login"),
                        onClick = { onLoginClick() },
                        content = { Icon(imageVector = Icons.AutoMirrored.Filled.Login) }
                    )
                }

                ActionBarMode.SEARCH -> Unit
                ActionBarMode.ADD,
                ActionBarMode.EDIT,
                ActionBarMode.DELETE,
                ActionBarMode.BATCH_DELETE -> {
                    if (write && layout != ActionBarLayout.NONE) {
                        Button(
                            modifier = Modifier.testTag(tag = "action_ok"),
                            onClick = {
                                onOkClick(mode)
                                onModeChange(ActionBarMode.DEFAULT)
                            },
                            content = { Icon(imageVector = Icons.Default.Check) }
                        )
                        Button(
                            modifier = Modifier.testTag(tag = "action_cancel"),
                            onClick = {
                                onCancelClick(mode)
                                onModeChange(ActionBarMode.DEFAULT)
                            },
                            content = { Icon(imageVector = Icons.Default.Close) }
                        )
                    }
                }
            }
        },
        secondaryActions = if (!actionsExpanded) null else {
            {
                when (mode) {
                    ActionBarMode.DEFAULT -> {
                        if (actionsExpanded && (layout == ActionBarLayout.ALL || layout == ActionBarLayout.LIST)) {
                            Button(
                                modifier = Modifier.testTag(tag = "action_search"),
                                onClick = {
                                    searchQuery = ""
                                    onModeChange(ActionBarMode.SEARCH)
                                },
                                content = { Icon(imageVector = Icons.Default.Search) }
                            )
                            Button(
                                modifier = Modifier.testTag(tag = "action_sort"),
                                onClick = { onSortAscendingClick(sortAscending) },
                                content = { Icon(imageVector = if (sortAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward) }
                            )
                            Box {
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
                            Box {
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
                        }
                    }

                    ActionBarMode.SEARCH -> {
                        if (layout == ActionBarLayout.ALL || layout == ActionBarLayout.LIST) {
                            Input(
                                modifier = Modifier
                                    .testTag(tag = "action_search_input")
                                    .padding(end = 4.dp),
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                maxLines = 1,
                                leadingIcon = { Icon(imageVector = Icons.Default.Search) }
                            )
                            Box {
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
                                    onModeChange(ActionBarMode.DEFAULT)
                                    searchQuery = ""
                                },
                                content = { Icon(imageVector = Icons.Default.Close) }
                            )
                        }
                    }

                    ActionBarMode.ADD,
                    ActionBarMode.EDIT,
                    ActionBarMode.DELETE,
                    ActionBarMode.BATCH_DELETE -> Unit
                }
            }
        }
    )
}

/** The display mode of the top action bar, shared across every screen that mounts it. */
enum class ActionBarMode {
    /** Default mode. The screen-appropriate layout is shown. */
    DEFAULT,
    /** Search mode. The search layout is shown. */
    SEARCH,
    /** Add mode. The confirmation and cancellation layout is shown. */
    ADD,
    /** Edit mode. The confirmation and cancellation layout is shown. */
    EDIT,
    /** Delete mode. The confirmation and cancellation layout is shown. */
    DELETE,
    /** Batch delete mode. The confirmation and cancellation layout is shown. */
    BATCH_DELETE,
}

/** Which set of action bar actions to show. */
enum class ActionBarLayout {
    NONE,
    LIST,
    DETAIL,
    ALL
}

@Preview
@Composable
private fun TopActionBarPreview() = Preview {
    TopActionBar(title = "None", onBackClick = {}, onLoginClick = {})
}

@Preview
@Composable
private fun TopActionBarAllPreview() = Preview {
    TopActionBar(
        title = "All",
        layout = ActionBarLayout.ALL,
        write = true,
        sortProperty = "name",
        properties = persistentMapOf(
            "name" to "Name",
            "type" to "Type",
        )
    )
}

@Preview
@Composable
private fun TopActionBarListPreview() = Preview {
    TopActionBar(
        title = "Monuments",
        layout = ActionBarLayout.LIST,
        write = true,
        sortProperty = "name",
        properties = persistentMapOf(
            "name" to "Name",
            "type" to "Type",
        )
    )
}

@Preview
@Composable
private fun TopActionBarDetailPreview() = Preview {
    TopActionBar(
        title = "Big Monument",
        layout = ActionBarLayout.DETAIL,
        write = true,
    )
}

@Preview
@Composable
private fun TopActionBarReadPreview() = Preview {
    TopActionBar(
        title = "Read",
        layout = ActionBarLayout.ALL,
        write = false,
        sortProperty = "name",
        properties = persistentMapOf(
            "name" to "Name",
            "type" to "Type",
        )
    )
}