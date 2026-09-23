package com.app.builder.ui.showcase.core.navigation

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.app.builder.ui.core.navigation.NavigationBar
import com.app.builder.ui.core.navigation.NavigationItem
import com.app.builder.ui.core.navigation.NavigationRail
import com.app.builder.ui.core.navigation.Tab
import com.app.builder.ui.core.navigation.Tabs
import com.app.builder.ui.core.overlay.Badge
import com.app.builder.ui.showcase.ShowcaseSection

/** Displays navigation bars, navigation rails in their various configurations, and tabs. */
@Composable
fun NavigationShowcase() {
    Column(
        modifier = Modifier.padding(all = 16.dp),
        verticalArrangement = Arrangement.spacedBy(space = 32.dp),
    ) {
        ShowcaseSection(title = "Navigation Bar") { NavigationBarPreview() }
        ShowcaseSection(title = "Navigation Rail") { NavigationRailPreview() }
        ShowcaseSection(title = "Navigation Rail Expanded") { NavigationRailExpandedPreview() }
        ShowcaseSection(title = "Navigation Rail Expandable") { NavigationRailExpandablePreview() }
        ShowcaseSection(title = "Navigation Rail with Overlay") { NavigationRailOverlayPreview() }
        ShowcaseSection(title = "Navigation Rail with Overlay Expanded") { NavigationRailOverlayExpandedPreview() }
        ShowcaseSection(title = "Navigation Rail with Overlay Expandable") { NavigationRailOverlayExpandablePreview() }
        ShowcaseSection(title = "Tabs") { TabsPreview() }
    }
}

/**
 * Builds the sample navigation items used by the previews.
 *
 * @return The sample [NavigationItem]s, with the selected one tracked across recompositions.
 */
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

@Composable
private fun NavigationBarPreview() {
    NavigationBar(items = getItems())
}

@Composable
private fun NavigationRailPreview() {
    Row(modifier = Modifier.height(height = 520.dp)) {
        NavigationRail(
            overlay = false,
            expanded = false,
            expandable = false,
            items = getItems(),
        )
    }
}

@Composable
private fun NavigationRailExpandedPreview() {
    Row(modifier = Modifier.height(height = 450.dp)) {
        NavigationRail(
            overlay = false,
            expanded = true,
            expandable = false,
            items = getItems(),
        )
    }
}

@Composable
private fun NavigationRailExpandablePreview() {
    Row(modifier = Modifier.height(height = 620.dp)) {
        NavigationRail(
            overlay = false,
            expanded = false,
            expandable = true,
            items = getItems(),
        )
        Text(modifier = Modifier.padding(top = 16.dp), text = "Sample Content")
    }
}

@Composable
private fun NavigationRailOverlayPreview() {
    Row(modifier = Modifier.height(height = 520.dp)) {
        NavigationRail(
            overlay = true,
            expanded = false,
            expandable = false,
            items = getItems(),
        )
    }
}

@Composable
private fun NavigationRailOverlayExpandedPreview() {
    Row(modifier = Modifier.height(height = 450.dp)) {
        NavigationRail(
            overlay = true,
            expanded = true,
            expandable = false,
            items = getItems(),
        )
    }
}

@Composable
private fun NavigationRailOverlayExpandablePreview() {
    Row(modifier = Modifier.height(height = 620.dp)) {
        NavigationRail(
            overlay = true,
            expanded = false,
            expandable = true,
            items = getItems(),
        )
        Text(modifier = Modifier.padding(top = 16.dp), text = "Sample Content")
    }
}

@Composable
private fun TabsPreview() {
    var selectedIndex by remember { mutableIntStateOf(value = 0) }
    val labels = listOf("Tab 1", "Tab 2", "Tab 3")
    Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
        Tabs(
            selectedTabIndex = selectedIndex,
            items = persistentListOf(
                *labels.mapIndexed { index, label ->
                    Tab(text = label, onClick = { selectedIndex = index })
                }.toTypedArray()
            )
        )
        Text(
            modifier = Modifier
                .padding(all = 8.dp)
                .align(alignment = Alignment.CenterHorizontally),
            text = "Content $selectedIndex"
        )
    }
}
