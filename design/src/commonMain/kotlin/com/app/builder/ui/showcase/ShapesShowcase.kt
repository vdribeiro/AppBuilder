package com.app.builder.ui.showcase

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.app.builder.ui.LocalColorScheme
import com.app.builder.ui.LocalShapes
import com.app.builder.ui.core.list.LazyColumn
import com.app.builder.ui.core.text.Text

/** A labeled corner shape, used to render a swatch. */
private data class ShapeSwatch(val label: String, val shape: Shape)

/** Displays every corner shape scale as a labeled list of filled swatches. */
@Composable
fun ShapesShowcase() {
    val colorScheme = LocalColorScheme.current
    val shapes = LocalShapes.current
    val swatches = listOf(
        ShapeSwatch(label = "Extra small (4dp)", shape = shapes.extraSmall),
        ShapeSwatch(label = "Small (8dp)", shape = shapes.small),
        ShapeSwatch(label = "Medium (12dp)", shape = shapes.medium),
        ShapeSwatch(label = "Large (16dp)", shape = shapes.large),
        ShapeSwatch(label = "Extra large (28dp)", shape = shapes.extraLarge),
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(all = 16.dp),
        verticalArrangement = Arrangement.spacedBy(space = 20.dp)
    ) {
        items(items = swatches) { swatch ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(space = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(size = 72.dp)
                        .background(color = colorScheme.primaryContainer, shape = swatch.shape)
                )
                Text(text = swatch.label, color = colorScheme.onSurface)
            }
        }
    }
}
