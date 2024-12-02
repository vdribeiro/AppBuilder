package com.app.builder.ui.core.button

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.app.builder.ui.LocalTypography
import com.app.builder.ui.Preview
import com.app.builder.ui.core.container.Scaffold
import com.app.builder.ui.core.image.Icon
import com.app.builder.ui.core.text.Text

/**
 * A dropdown component that decouples the underlying menu layout from the data model by using functional extraction lambdas to resolve state, text labels, visibility behaviors, and leading icons for each option.
 *
 * @param modifier The [Modifier] to be applied to the dropdown menu container.
 * @param expanded Controls whether the dropdown menu is currently visible on screen.
 * @param onDismissRequest Called when the user taps outside the menu bounds or presses back to close it.
 * @param items An [ImmutableList] of [DropdownItem]s for each choice item.
 */
@Composable
fun Dropdown(
    modifier: Modifier = Modifier,
    expanded: Boolean = false,
    onDismissRequest: () -> Unit = {},
    items: ImmutableList<DropdownItem> = persistentListOf(),
) {
    val typography = LocalTypography.current

    DropdownMenu(
        modifier = modifier,
        expanded = expanded,
        onDismissRequest = onDismissRequest,
    ) {
        items.forEach { item ->
            DropdownMenuItem(
                enabled = item.enabled,
                text = {
                    Text(
                        text = item.text,
                        maxLines = 1,
                        style = typography.labelLarge
                    )
                },
                onClick = item.onClick,
                leadingIcon = item.leadingIcon
            )
        }
    }
}

/**
 * Dropdown item.
 *
 * @property enabled Whether a specific item choice can be selected. Defaults to `true`.
 * @property text The text label string for a given item choice. Defaults to empty.
 * @property onClick A callback invoked when its option row is clicked.
 * @property leadingIcon An optional slot-based composable to render as a leading icon. A fallback empty spacing icon is applied.
 */
@Stable
data class DropdownItem(
    val enabled: Boolean = true,
    val text: String = "",
    val onClick: () -> Unit = {},
    val leadingIcon: @Composable () -> Unit = { Icon(emptySize = 8.dp) }
)

@Preview
@Composable
private fun DropdownPreview() = Preview {
    Scaffold(
        content = { innerPadding ->
            Box(
                modifier = Modifier.padding(paddingValues = innerPadding),
                contentAlignment = Alignment.TopStart
            ) {
                Dropdown(
                    expanded = true,
                    items = persistentListOf(
                        DropdownItem(
                            enabled = true,
                            text = "Item 1",
                            leadingIcon = { Icon(imageVector = Icons.Default.Apps) }
                        ),
                        DropdownItem(
                            enabled = false,
                            text = "Item 2",
                            leadingIcon = { Icon(imageVector = Icons.Default.Apps) }
                        ),
                        DropdownItem(
                            enabled = true,
                            text = "Item 3",
                        ),
                        DropdownItem(
                            enabled = true,
                            text = "Item 4",
                        ),
                    ),
                )
            }
        }
    )
}