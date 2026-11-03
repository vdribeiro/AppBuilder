package com.app.builder.ui.showcase

import kotlin.math.roundToInt
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.builder.data.theme.ThemeCache
import com.app.builder.data.theme.CustomTheme
import com.app.builder.ui.LocalColorScheme
import com.app.builder.ui.ShapeRole
import com.app.builder.ui.component.overlay.ShapePickerDialog
import com.app.builder.ui.core.list.LazyColumn
import com.app.builder.ui.core.text.Text

/**
 * Displays every corner shape scale as a labeled list of filled swatches.
 * Clicking a swatch opens a [ShapePickerDialog] that replaces the shape's corner radius in the [ThemeCache].
 *
 * @param onThemeChange Invoked with the updated theme whenever a corner radius is applied or reset.
 */
@Composable
fun ShapesShowcase(onThemeChange: (CustomTheme) -> Unit = {}) {
    val colorScheme = LocalColorScheme.current
    val customTheme by ThemeCache.cacheState.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<ShapeRole?>(value = null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(all = 16.dp),
        verticalArrangement = Arrangement.spacedBy(space = 20.dp)
    ) {
        items(items = ShapeRole.entries) { role ->
            val radius = customTheme.shapes[role.name]?.dp ?: role.radius
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { editing = role }
                    .testTag(tag = "shape_swatch_${role.name}"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(space = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(size = 72.dp)
                        .background(color = colorScheme.primaryContainer, shape = RoundedCornerShape(size = radius))
                )
                Text(text = "${role.text} (${radius.value.roundToInt()}dp)", color = colorScheme.onSurface)
            }
        }
    }

    editing?.let { role ->
        ShapePickerDialog(
            title = role.text,
            radius = customTheme.shapes[role.name]?.dp ?: role.radius,
            onConfirm = { radius ->
                editing = null
                onThemeChange(ThemeCache.setShape(role = role.name, radius = radius.value))
            },
            onReset = {
                editing = null
                onThemeChange(ThemeCache.setShape(role = role.name, radius = null))
            },
            onDismiss = { editing = null }
        )
    }
}
