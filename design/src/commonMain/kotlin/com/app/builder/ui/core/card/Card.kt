package com.app.builder.ui.core.card

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.app.builder.ui.LocalColorScheme
import com.app.builder.ui.LocalShapes
import com.app.builder.ui.Preview
import com.app.builder.ui.core.text.Text
import androidx.compose.material3.Card as MaterialCard

/**
 * Card component that provides a pre-configured small-radius shape and light elevation. It alters
 * its background container color and draws an outer outline border dynamically depending on its [selected] selection state.
 *
 * @param modifier The [Modifier] to be applied to the outer layout bounds of the card.
 * @param selected Controls the visual emphasis state. When `true`, switches the container theme color and applies a prominent border stroke.
 * @param content A composable slot block executing inside the card's sequential [ColumnScope].
 */
@Composable
fun Card(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    content: @Composable ColumnScope.() -> Unit = {}
) {
    val colorScheme = LocalColorScheme.current
    val shapes = LocalShapes.current
    val colors = when {
        selected -> CardDefaults.cardColors(containerColor = colorScheme.onSecondary)
        else -> CardDefaults.cardColors()
    }
    val border = when {
        selected -> BorderStroke(width = 2.dp, color = colorScheme.outline)
        else -> null
    }

    MaterialCard(
        modifier = modifier,
        shape = shapes.small,
        colors = colors,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = border,
        content = content
    )
}

@Preview
@Composable
private fun CardPreview() = Preview {
    var selected by remember { mutableStateOf(value = false) }
    Card(
        modifier = Modifier.clickable { selected = !selected },
        selected = selected,
        content = {
            Text(
                modifier = Modifier.padding(all = 16.dp),
                text = "Tap. Selected: $selected"
            )
        }
    )
}
