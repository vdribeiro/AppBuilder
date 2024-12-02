package com.app.builder.ui.core.navigation

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuOpen
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.WideNavigationRail
import androidx.compose.material3.WideNavigationRailItem
import androidx.compose.material3.WideNavigationRailValue
import androidx.compose.material3.rememberWideNavigationRailState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.app.builder.ui.LocalTypography
import com.app.builder.ui.Preview
import com.app.builder.ui.core.button.Button
import com.app.builder.ui.core.button.ButtonStyle
import com.app.builder.ui.core.image.Icon
import com.app.builder.ui.core.list.ListItem
import com.app.builder.ui.core.overlay.Badge
import com.app.builder.ui.core.overlay.BadgedBox
import com.app.builder.ui.core.overlay.ModalBottomSheet
import com.app.builder.ui.core.text.Text
import androidx.compose.ui.Alignment as UiAlignment
import com.app.builder.ui.core.list.LazyColumn as ScrollableLazyColumn

/**
 * Navigation item.
 *
 * @property enabled Whether a specific item is interactive. Defaults to `true`.
 * @property selected Whether a specific item is currently selected. Defaults to `false`.
 * @property text The text label string for a given item. Defaults to blank.
 * @property icon The [ImageVector] asset for a given item. Defaults to null.
 * @property badge An optional composable slot rendering an unread-count or unseen-event indicator anchored to the icon. Defaults to null.
 * @property onClick A callback invoked when an item is selected by the user.
 */
@Stable
data class NavigationItem(
    val enabled: Boolean = true,
    val selected: Boolean = false,
    val text: String = "",
    val icon: ImageVector? = null,
    val badge: (@Composable () -> Unit)? = null,
    val onClick: () -> Unit = {}
)

/**
 * Navigation bar for a persistent and convenient way to switch between destinations.
 *
 * @param modifier The [Modifier] to be applied to the root layout.
 * @param maxItems The maximum number of destinations rendered directly before the rest overflow into a "More" entry. Defaults to `5`.
 * @param items An [ImmutableList] of [NavigationItem]s for each navigation tab.
 */
@Composable
fun NavigationBar(
    modifier: Modifier = Modifier,
    maxItems: Int = 5,
    items: ImmutableList<NavigationItem> = persistentListOf(),
) {
    val typography = LocalTypography.current
    var moreExpanded by remember { mutableStateOf(value = false) }
    val overflowItems = if (items.size > maxItems) items.drop(n = maxItems - 1) else emptyList()
    val visibleItems = if (items.size > maxItems) items.take(n = maxItems - 1) else items

    NavigationBar(modifier = modifier.fillMaxWidth()) {
        visibleItems.forEach { item ->
            NavigationBarItem(
                enabled = item.enabled,
                selected = item.selected,
                label = {
                    Text(
                        text = item.text,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        style = typography.labelLarge
                    )
                },
                icon = {
                    val iconContent: @Composable () -> Unit = { Icon(imageVector = item.icon, contentDescription = item.text, emptySize = 12.dp) }
                    if (item.badge != null) {
                        BadgedBox(badge = { item.badge.invoke() }, content = { iconContent() })
                    } else {
                        iconContent()
                    }
                },
                onClick = item.onClick,
            )
        }
        if (overflowItems.isNotEmpty()) {
            NavigationBarItem(
                selected = false,
                label = { Text(text = "more", textAlign = TextAlign.Center, maxLines = 1, style = typography.labelLarge) },
                icon = { Icon(imageVector = Icons.Default.MoreHoriz, contentDescription = "more") },
                onClick = { moreExpanded = true },
            )
        }
    }

    if (moreExpanded) NavigationOverflowSheet(items = overflowItems, maxItems = 7, onDismissRequest = { moreExpanded = false })
}

/**
 * Navigation rail for a persistent and convenient way to switch between destinations.
 *
 * @param modifier The [Modifier] to be applied to the root layout.
 * @param overlay Whether the bar should overlay the content. Defaults to `false`.
 * @param expanded Whether the bar is expanded. Defaults to `false`.
 * @param expandable Whether the bar can be expanded. Defaults to `false`.
 * @param maxItems The maximum number of destinations rendered directly before the rest overflow into a "More" entry. Defaults to `7`.
 * @param items An [ImmutableList] of [NavigationItem]s for each navigation tab.
 */
@Composable
fun NavigationRail(
    modifier: Modifier = Modifier,
    overlay: Boolean = false,
    expanded: Boolean = false,
    expandable: Boolean = false,
    maxItems: Int = 7,
    items: ImmutableList<NavigationItem> = persistentListOf(),
) {
    val typography = LocalTypography.current

    var expanded by remember { mutableStateOf(value = expanded) }
    var moreExpanded by remember { mutableStateOf(value = false) }
    val overflowItems = if (items.size > maxItems) items.drop(n = maxItems - 1) else emptyList()
    val visibleItems = if (items.size > maxItems) items.take(n = maxItems - 1) else items

    val state = rememberWideNavigationRailState(
        initialValue = if (expanded) WideNavigationRailValue.Expanded else WideNavigationRailValue.Collapsed
    )
    LaunchedEffect(key1 = expanded) {
        if (expanded) state.expand() else state.collapse()
    }

    WideNavigationRail(
        modifier = modifier
            .fillMaxHeight()
            .then(
                other = if (!overlay) Modifier else {
                    Modifier.zIndex(zIndex = 1f).layout { measurable, constraints ->
                        val collapsedWidthPx = 96.dp.roundToPx()
                        val placeable = measurable.measure(constraints = constraints.copy(minWidth = 0, maxWidth = Constraints.Infinity))
                        layout(width = collapsedWidthPx, height = placeable.height) { placeable.placeRelative(x = 0, y = 0) }
                    }
                }
            ),
        state = state,
        header = {
            if (expandable) Box(modifier = Modifier.width(width = 96.dp), contentAlignment = UiAlignment.Center) {
                Button(
                    style = ButtonStyle.TEXT,
                    onClick = { expanded = !expanded },
                    content = { Icon(imageVector = if (expanded) Icons.AutoMirrored.Filled.MenuOpen else Icons.Default.Menu) }
                )
            }
        }
    ) {
        visibleItems.forEach { item ->
            WideNavigationRailItem(
                enabled = item.enabled,
                selected = item.selected,
                railExpanded = expanded,
                label = {
                    Text(
                        text = item.text,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        style = typography.labelLarge
                    )
                },
                icon = {
                    val iconContent: @Composable () -> Unit = { Icon(imageVector = item.icon, contentDescription = item.text, emptySize = 12.dp) }
                    if (item.badge != null) {
                        BadgedBox(badge = { item.badge.invoke() }, content = { iconContent() })
                    } else {
                        iconContent()
                    }
                },
                onClick = item.onClick,
            )
        }
        if (overflowItems.isNotEmpty()) {
            WideNavigationRailItem(
                selected = false,
                railExpanded = expanded,
                label = { Text(text = "more", textAlign = TextAlign.Center, maxLines = 1, style = typography.labelLarge) },
                icon = { Icon(imageVector = Icons.Default.MoreVert, contentDescription = "more") },
                onClick = { moreExpanded = true },
            )
        }
    }

    if (moreExpanded) NavigationOverflowSheet(items = overflowItems, maxItems = 7, onDismissRequest = { moreExpanded = false })
}

/**
 * A [ModalBottomSheet] listing destinations that overflowed a [NavigationBar] or [NavigationRail].
 *
 * @param items An [ImmutableList] of [NavigationItem]s for each navigation tab.
 * @param maxItems The maximum number of destinations for the overflow to show in full screen.
 * @param onDismissRequest Callback invoked when the overlay is dismissed by tapping outside or dragging down completely.
 */
@Composable
private fun NavigationOverflowSheet(items: List<NavigationItem>, maxItems: Int, onDismissRequest: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismissRequest, fullScreen = items.size > maxItems) {
        ScrollableLazyColumn {
            items(items = items) { item ->
                ListItem(
                    modifier = Modifier.clickable(enabled = item.enabled) {
                        item.onClick()
                        onDismissRequest()
                    },
                    headlineText = item.text,
                    leadingContent = { Icon(imageVector = item.icon, contentDescription = item.text) }
                )
            }
        }
    }
}

@Composable
private fun getItems(): PersistentList<NavigationItem> {
    var selectedIndex by remember { mutableIntStateOf(value = 0) }
    val labels = listOf(
        "Home",
        "Search",
        "",
        "Notifications",
        "Messages",
        "Profile",
        "Settings",
        "Favorites",
        "History",
        "Help"
    )
    return persistentListOf(
        *labels.mapIndexed { index, label ->
            NavigationItem(
                text = label,
                icon = Icons.Default.Apps,
                selected = index == selectedIndex,
                onClick = { selectedIndex = index },
                badge = if (index == 4) {
                    { Badge(text = "3") }
                } else null
            )
        }.toTypedArray()
    )
}

@Preview
@Composable
private fun NavigationBarPreview() = Preview {
    NavigationBar(items = getItems())
}

@Preview
@Composable
private fun NavigationRailPreview() = Preview {
    NavigationRail(
        overlay = false,
        expanded = false,
        expandable = false,
        items = getItems(),
    )
}

@Preview
@Composable
private fun NavigationRailExpandedPreview() = Preview {
    NavigationRail(
        overlay = false,
        expanded = true,
        expandable = false,
        items = getItems(),
    )
}

@Preview
@Composable
private fun NavigationRailExpandablePreview() = Preview {
    NavigationRail(
        overlay = false,
        expanded = false,
        expandable = true,
        items = getItems(),
    )
}

@Preview
@Composable
private fun NavigationRailOverlayPreview() = Preview {
    NavigationRail(
        overlay = true,
        expanded = false,
        expandable = false,
        items = getItems(),
    )
}

@Preview
@Composable
private fun NavigationRailOverlayExpandedPreview() = Preview {
    NavigationRail(
        overlay = true,
        expanded = true,
        expandable = false,
        items = getItems(),
    )
}

@Preview
@Composable
private fun NavigationRailOverlayExpandablePreview() = Preview {
    NavigationRail(
        overlay = true,
        expanded = false,
        expandable = true,
        items = getItems(),
    )
}
