package com.app.builder.ui.showcase

import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.SmartButton
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewDay
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.vector.ImageVector
import com.app.builder.ui.core.list.LazyColumn
import com.app.builder.ui.core.navigation.NavigationItem
import com.app.builder.ui.core.navigation.NavigationRail
import com.app.builder.ui.showcase.core.bar.TopBarShowcase
import com.app.builder.ui.showcase.core.button.ButtonShowcase
import com.app.builder.ui.showcase.core.card.CardShowcase
import com.app.builder.ui.showcase.core.divider.DividerShowcase
import com.app.builder.ui.showcase.core.image.ImageShowcase
import com.app.builder.ui.showcase.core.list.ListShowcase
import com.app.builder.ui.showcase.core.navigation.NavigationShowcase
import com.app.builder.ui.showcase.core.overlay.OverlayShowcase
import com.app.builder.ui.showcase.core.progress.ProgressShowcase
import com.app.builder.ui.showcase.core.text.TextShowcase

/** The items for the rail's destinations and the scrollable content below it in display order. */
private val railSections: List<Triple<String, ImageVector, @Composable () -> Unit>> = listOf(
    Triple("Button", Icons.Default.SmartButton, { ButtonShowcase() }),
    Triple("Progress", Icons.Default.Autorenew, { ProgressShowcase() }),
    Triple("Text", Icons.Default.Edit, { TextShowcase() }),
    Triple("Image", Icons.Default.Image, { ImageShowcase() }),
    Triple("Navigation", Icons.Default.Navigation, { NavigationShowcase() }),
    Triple("Top Bar", Icons.Default.ViewDay, { TopBarShowcase() }),
    Triple("Overlay", Icons.Default.ViewAgenda, { OverlayShowcase() }),
    Triple("Card", Icons.Default.CreditCard, { CardShowcase() }),
    Triple("Lists", Icons.AutoMirrored.Filled.List, { ListShowcase() }),
    Triple("Divider", Icons.Default.HorizontalRule, { DividerShowcase() }),
)

@Composable
fun ComponentsShowcase() {
    val state = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val selectedRailIndex by remember { derivedStateOf { state.firstVisibleItemIndex.coerceIn(minimumValue = 0, maximumValue = railSections.lastIndex) } }
    Row {
        NavigationRail(
            items = persistentListOf(
                *railSections.mapIndexed { index, (label, icon, _) ->
                    NavigationItem(
                        text = label,
                        icon = icon,
                        selected = index == selectedRailIndex,
                        onClick = { coroutineScope.launch { state.animateScrollToItem(index = index) } }
                    )
                }.toTypedArray()
            )
        )
        LazyColumn(state = state) {
            railSections.forEach { (title, _, content) ->
                item { CategorySection(title = title, content = content) }
            }
        }
    }
}