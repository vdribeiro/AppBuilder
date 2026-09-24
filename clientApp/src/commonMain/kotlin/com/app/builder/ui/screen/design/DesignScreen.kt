package com.app.builder.ui.screen.design

import kotlinx.collections.immutable.toPersistentList
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.FormatShapes
import androidx.compose.material.icons.filled.SettingsInputComponent
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import com.app.builder.ui.Preview
import com.app.builder.ui.core.bar.TopBar
import com.app.builder.ui.core.navigation.NavigationBar
import com.app.builder.ui.core.navigation.NavigationItem
import com.app.builder.ui.core.text.Text
import com.app.builder.ui.navigation.LocalRouter
import com.app.builder.ui.navigation.Router
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.screen.Screen
import com.app.builder.ui.showcase.ColorsShowcase
import com.app.builder.ui.showcase.ComponentsShowcase
import com.app.builder.ui.showcase.ShapesShowcase
import com.app.builder.ui.showcase.TypographyShowcase

/**
 * The sections of the design showcase, in display order.
 *
 * @property text The label shown in the design navigation bar.
 * @property icon The icon shown in the design navigation bar.
 */
enum class DesignSection(val text: String, val icon: ImageVector) {
    COMPONENTS(text = "Components", icon = Icons.Default.SettingsInputComponent),
    COLORS(text = "Colors", icon = Icons.Default.Colorize),
    SHAPES(text = "Shapes", icon = Icons.Default.FormatShapes),
    TYPOGRAPHY(text = "Typography", icon = Icons.Default.FontDownload),
}

/**
 * A section of the design showcase, a self contained catalog of the design system rendered as its own section of the app, with its own navigation bar.
 *
 * @param modifier [Modifier] applied to the root container.
 * @param section The section to display.
 * @param onSectionClick Invoked with the section to navigate to when another section is selected.
 * @param onAppClick Invoked when the user leaves the design showcase and returns to the app.
 */
@Composable
fun DesignScreen(
    modifier: Modifier = Modifier,
    section: DesignSection,
    onSectionClick: (DesignSection) -> Unit = {},
    onAppClick: () -> Unit = {},
) {
    val router = LocalRouter.current

    Screen(
        modifier = modifier,
        onBackClick = { router.navigate(screen = Screen.Home, option = Router.NavOption.CLEAR) },
        topBar = { TopBar(title = { Text(text = "Design Showcase", translate = false) }) },
        bottomBar = {
            DesignNavigation(
                section = section,
                onSectionClick = onSectionClick,
                onAppClick = onAppClick
            )
        },
    ) {
        when (section) {
            DesignSection.COMPONENTS -> ComponentsShowcase()
            DesignSection.COLORS -> ColorsShowcase()
            DesignSection.SHAPES -> ShapesShowcase()
            DesignSection.TYPOGRAPHY -> TypographyShowcase()
        }
    }
}

/**
 * The design showcase's navigation bar, holding every [DesignSection] plus the entry that leaves the showcase and returns to the app.
 *
 * @param modifier [Modifier] applied to the navigation bar.
 * @param section The currently displayed section.
 * @param onSectionClick Invoked with the section to navigate to when another section is selected.
 * @param onAppClick Invoked when the user leaves the design showcase and returns to the app.
 */
@Composable
fun DesignNavigation(
    modifier: Modifier = Modifier,
    section: DesignSection,
    onSectionClick: (DesignSection) -> Unit = {},
    onAppClick: () -> Unit = {},
) {
    val items = buildList {
        add(
            element = NavigationItem(
                text = "App",
                icon = Icons.Default.Apps,
                onClick = onAppClick
            )
        )
        DesignSection.entries.forEach { entry ->
            add(
                element = NavigationItem(
                    selected = entry == section,
                    text = entry.text,
                    icon = entry.icon,
                    onClick = { onSectionClick(entry) }
                )
            )
        }
    }.toPersistentList()

    NavigationBar(
        modifier = modifier.testTag(tag = "design_navigation_bar"),
        maxItems = items.size,
        items = items
    )
}

@Preview
@Composable
private fun DesignScreenPreview() = Preview {
    DesignScreen(section = DesignSection.TYPOGRAPHY)
}
