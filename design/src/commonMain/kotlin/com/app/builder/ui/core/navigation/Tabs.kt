package com.app.builder.ui.core.navigation

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.app.builder.ui.LocalTypography
import com.app.builder.ui.Preview
import com.app.builder.ui.core.text.Text
import com.app.builder.ui.core.text.TextValue
import com.app.builder.ui.core.text.toTextValue

/**
 * Tab item.
 *
 * @property enabled Whether a specific tab is interactive. Defaults to `true`.
 * @property text The text label for a given tab. Defaults to null.
 * @property onClick A callback invoked when the tab is selected by the user.
 */
@Stable
data class Tab(
    val enabled: Boolean = true,
    val text: TextValue? = null,
    val onClick: () -> Unit = {}
) {
    /**
     * Creates a tab whose text label is given as a translation key.
     *
     * @param enabled Whether the tab is interactive. Defaults to `true`.
     * @param text The translation key of the text label.
     * @param onClick A callback invoked when the tab is selected by the user.
     */
    constructor(
        enabled: Boolean = true,
        text: String?,
        onClick: () -> Unit = {}
    ): this(
        enabled = enabled,
        text = text?.toTextValue(),
        onClick = onClick
    )
}

/**
 * A row of destinations grouping related content within the same context, drawing an animated indicator beneath the currently [selectedTabIndex].
 *
 * @param modifier The [Modifier] to be applied to the row's layout.
 * @param selectedTabIndex The index of the currently selected tab within [items].
 * @param items An [ImmutableList] of [Tab]s for each destination.
 */
@Composable
fun Tabs(
    modifier: Modifier = Modifier,
    selectedTabIndex: Int = 0,
    items: ImmutableList<Tab> = persistentListOf(),
) {
    val typography = LocalTypography.current

    PrimaryTabRow(modifier = modifier, selectedTabIndex = selectedTabIndex) {
        items.forEachIndexed { index, item ->
            Tab(
                selected = index == selectedTabIndex,
                enabled = item.enabled,
                onClick = item.onClick,
                text = {
                    Text(
                        value = item.text ?: TextValue.Plain(text = ""),
                        maxLines = 1,
                        style = typography.labelLarge
                    )
                }
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun TabsPreview() = Preview {
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
