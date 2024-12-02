package com.app.builder

import kotlinx.collections.immutable.persistentListOf
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.FormatShapes
import androidx.compose.material.icons.filled.InsertEmoticon
import androidx.compose.material.icons.filled.SettingsInputComponent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.app.builder.ui.AppTheme
import com.app.builder.ui.ColorsShowcase
import com.app.builder.ui.ComponentsShowcase
import com.app.builder.ui.LocalTypography
import com.app.builder.ui.ShapesShowcase
import com.app.builder.ui.TypographyShowcase
import com.app.builder.ui.core.bar.TopBar
import com.app.builder.ui.core.container.Scaffold
import com.app.builder.ui.core.divider.Divider
import com.app.builder.ui.core.navigation.NavigationBar
import com.app.builder.ui.core.navigation.NavigationItem
import com.app.builder.ui.core.text.Text

/** The items for the bottom navigation bar in display order. */
private val navigationScreens: List<Triple<String, ImageVector, @Composable () -> Unit>> = listOf(
    Triple("Components", Icons.Default.SettingsInputComponent, { ComponentsShowcase() }),
    Triple("Icons", Icons.Default.InsertEmoticon, { ComponentsShowcase() }),
    Triple("Colors", Icons.Default.Colorize, { ColorsShowcase() }),
    Triple("Shapes", Icons.Default.FormatShapes, { ShapesShowcase() }),
    Triple("Typography", Icons.Default.FontDownload, { TypographyShowcase() }),
)

/** App entry point. */
@Composable
fun App() {
    AppTheme {
        var selectedContentIndex by remember { mutableIntStateOf(value = 0) }
        Scaffold(
            topBar = { TopBar(title = { Text(text = "Design Showcase") }) },
            bottomBar = {
                NavigationBar(
                    items = persistentListOf(
                        *navigationScreens.mapIndexed { index, (label, icon, _) ->
                            NavigationItem(
                                text = label,
                                icon = icon,
                                selected = index == selectedContentIndex,
                                onClick = { selectedContentIndex = index }
                            )
                        }.toTypedArray()
                    )
                )
            },
            content = { innerPadding ->
                Box(modifier = Modifier.padding(paddingValues = innerPadding)) {
                    navigationScreens[selectedContentIndex].third()
                }
            }
        )
    }
}

/** A category with a title, content and divider. */
@Composable
fun CategorySection(title: String, content: @Composable () -> Unit = {}) {
    val typography = LocalTypography.current
    Column {
        Text(text = title, style = typography.headlineSmall)
        content()
        Divider(modifier = Modifier.padding(top = 16.dp, bottom = 16.dp))
    }
}

/** A section with a title and content. */
@Composable
fun ShowcaseSection(title: String, content: @Composable () -> Unit) {
    val typography = LocalTypography.current
    Column(verticalArrangement = Arrangement.spacedBy(space = 12.dp)) {
        Text(text = title, style = typography.titleMedium)
        content()
    }
}
