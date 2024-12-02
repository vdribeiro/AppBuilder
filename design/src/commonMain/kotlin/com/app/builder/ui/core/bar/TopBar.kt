package com.app.builder.ui.core.bar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.app.builder.ui.LocalColorScheme
import com.app.builder.ui.Preview
import com.app.builder.ui.core.button.Button
import com.app.builder.ui.core.image.Icon

/** Distinguishes between the layout treatments a [TopBar] can adopt. */
enum class TopBarVariant {
    /** A single-row bar with a compact title. */
    SMALL,
    /** A two-row bar with a larger title. */
    MEDIUM,
    /** A two-row bar with the most prominent title treatment. */
    LARGE
}

/**
 * A top header surface that standardizes the navigation structure across target windows by defining distinct slot boundaries for title branding, secondary multi-action configurations, and primary backwards navigation hooks.
 *
 * @param modifier The [Modifier] to be applied to the top app bar structural container.
 * @param variant The layout treatment to render. Defaults to [TopBarVariant.SMALL].
 * @param title A slot layout block responsible for rendering header content labels or page identifiers.
 * @param navigationIcon A slot layout block positioned at the start edge of the bar, typically embedding a back arrow button or hamburger menu trigger.
 * @param actions A sequential horizontal slot container dedicated to secondary tool shortcuts.
 * @param secondaryActions An optional second row of actions rendered below the bar, right-aligned. When `null`, no second row is rendered.
 */
@Composable
fun TopBar(
    modifier: Modifier = Modifier,
    variant: TopBarVariant = TopBarVariant.SMALL,
    title: @Composable () -> Unit = {},
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    secondaryActions: (@Composable RowScope.() -> Unit)? = null,
) {
    Column(modifier = modifier) {
        when (variant) {
            TopBarVariant.SMALL -> TopAppBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height = 64.dp),
                title = title,
                navigationIcon = navigationIcon,
                actions = actions
            )

            TopBarVariant.MEDIUM -> MediumTopAppBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height = 64.dp),
                title = title,
                navigationIcon = navigationIcon,
                actions = actions
            )

            TopBarVariant.LARGE -> LargeTopAppBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height = 64.dp),
                title = title,
                navigationIcon = navigationIcon,
                actions = actions
            )
        }
        if (secondaryActions != null) {
            val colorScheme = LocalColorScheme.current
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height = 64.dp)
                    .background(color = colorScheme.surface)
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CompositionLocalProvider(value = LocalContentColor provides colorScheme.onSurfaceVariant) {
                    secondaryActions()
                }
            }
        }
    }
}

@Preview
@Composable
private fun TopBarPreview() = Preview {
    TopBar(
        variant = TopBarVariant.SMALL,
        title = { Text(text = "Title") },
        navigationIcon = { Button(enabled = true, onClick = {}, content = { Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack) }) },
        actions = {
            Button(enabled = true, onClick = {}, content = { Icon(imageVector = Icons.Default.Apps) })
            Button(enabled = true, onClick = {}, content = { Icon(imageVector = Icons.Default.Search) })
        }
    )
}

@Preview
@Composable
private fun TopBarMediumPreview() = Preview {
    TopBar(
        variant = TopBarVariant.MEDIUM,
        title = { Text(text = "Title") },
        navigationIcon = { Button(enabled = true, onClick = {}, content = { Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack) }) },
        actions = {
            Button(enabled = true, onClick = {}, content = { Icon(imageVector = Icons.Default.Apps) })
            Button(enabled = true, onClick = {}, content = { Icon(imageVector = Icons.Default.Search) })
        }
    )
}

@Preview
@Composable
private fun TopBarLargePreview() = Preview {
    TopBar(
        variant = TopBarVariant.LARGE,
        title = { Text(text = "Title") },
        navigationIcon = { Button(enabled = true, onClick = {}, content = { Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack) }) },
        actions = {
            Button(enabled = true, onClick = {}, content = { Icon(imageVector = Icons.Default.Apps) })
            Button(enabled = true, onClick = {}, content = { Icon(imageVector = Icons.Default.Search) })
        }
    )
}

@Preview
@Composable
private fun TopBarSecondaryActionsPreview() = Preview {
    TopBar(
        variant = TopBarVariant.SMALL,
        title = { Text(text = "Title") },
        navigationIcon = { Button(enabled = true, onClick = {}, content = { Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack) }) },
        actions = {
            Button(enabled = true, onClick = {}, content = { Icon(imageVector = Icons.Default.Apps) })
            Button(enabled = true, onClick = {}, content = { Icon(imageVector = Icons.Default.Search) })
        },
        secondaryActions = {
            Button(enabled = true, onClick = {}, content = { Icon(imageVector = Icons.Default.Search) })
        }
    )
}

@Preview
@Composable
private fun TopBarMediumSecondaryActionsPreview() = Preview {
    TopBar(
        variant = TopBarVariant.MEDIUM,
        title = { Text(text = "Title") },
        navigationIcon = { Button(enabled = true, onClick = {}, content = { Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack) }) },
        actions = {
            Button(enabled = true, onClick = {}, content = { Icon(imageVector = Icons.Default.Apps) })
            Button(enabled = true, onClick = {}, content = { Icon(imageVector = Icons.Default.Search) })
        },
        secondaryActions = {
            Button(enabled = true, onClick = {}, content = { Icon(imageVector = Icons.Default.Search) })
        }
    )
}

@Preview
@Composable
private fun TopBarLargeSecondaryActionsPreview() = Preview {
    TopBar(
        variant = TopBarVariant.LARGE,
        title = { Text(text = "Title") },
        navigationIcon = { Button(enabled = true, onClick = {}, content = { Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack) }) },
        actions = {
            Button(enabled = true, onClick = {}, content = { Icon(imageVector = Icons.Default.Apps) })
            Button(enabled = true, onClick = {}, content = { Icon(imageVector = Icons.Default.Search) })
        },
        secondaryActions = {
            Button(enabled = true, onClick = {}, content = { Icon(imageVector = Icons.Default.Search) })
        }
    )
}
