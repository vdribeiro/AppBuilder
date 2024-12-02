package com.app.builder.ui.core.button

import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedIconToggleButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.app.builder.ui.LocalTypography
import com.app.builder.ui.Preview
import com.app.builder.ui.core.image.Icon
import com.app.builder.ui.core.progress.showLoading
import com.app.builder.ui.core.text.Text
import androidx.compose.material3.Button as MaterialButton
import androidx.compose.material3.IconToggleButton as MaterialIconToggleButton

/** Distinguishes between the container emphasis treatments a [Button] can adopt. */
enum class ButtonStyle {
    /** Highest emphasis, solid container color. Used for the single most important action on a screen. */
    FILLED,
    /** High emphasis with a lower-contrast container, an alternative to [FILLED] when several buttons compete for attention. */
    FILLED_TONAL,
    /** High emphasis with a shadow, used when a button needs to visually stand out from its background. */
    ELEVATED,
    /** Medium emphasis, bordered container. Used for important but not primary actions. */
    OUTLINED,
    /** Lowest emphasis, no container. Used for the least important actions, such as a dialog's dismiss button. */
    TEXT
}

/**
 * A button with a built-in loading state.
 * When [loading] is `true`, the content is swapped for a [CircularProgressIndicator] and the button automatically becomes non-interactive.
 *
 * @param modifier The [Modifier] to be applied to the button layout.
 * @param style The container emphasis treatment to render. Defaults to [ButtonStyle.OUTLINED].
 * @param loading Controls whether to show a loading wheel instead of the text label.
 * @param enabled Controls the enabled state of the button. When `false`, the button is not clickable.
 * @param text The text label to display inside the button.
 * @param checked Whether this button is currently toggled on.
 * @param content A composable slot rendering an icon inside the button.
 * @param onClick Will be called when the user clicks the button, with the requested new checked state if checked is not null.
 */
@Composable
fun Button(
    modifier: Modifier = Modifier,
    style: ButtonStyle = ButtonStyle.OUTLINED,
    loading: Boolean = false,
    enabled: Boolean = true,
    text: String? = null,
    checked: Boolean? = null,
    content: (@Composable () -> Unit)? = null,
    onClick: (Boolean) -> Unit = {},
) {
    val typography = LocalTypography.current
    val isEnabled = enabled && !loading
    when (text) {
        null if content != null && checked != null -> {
            val content: @Composable () -> Unit = { if (showLoading(loading = loading)) CircularProgressIndicator(modifier = Modifier.size(size = 24.dp)) else content() }
            when (style) {
                ButtonStyle.FILLED -> FilledIconToggleButton(modifier = modifier, checked = checked, onCheckedChange = onClick, enabled = enabled, content = content)
                ButtonStyle.FILLED_TONAL -> FilledTonalIconToggleButton(modifier = modifier, checked = checked, onCheckedChange = onClick, enabled = enabled, content = content)
                ButtonStyle.ELEVATED -> MaterialIconToggleButton(modifier = modifier, checked = checked, onCheckedChange = onClick, enabled = enabled, content = content)
                ButtonStyle.OUTLINED -> OutlinedIconToggleButton(modifier = modifier, checked = checked, onCheckedChange = onClick, enabled = enabled, content = content)
                ButtonStyle.TEXT -> MaterialIconToggleButton(modifier = modifier, checked = checked, onCheckedChange = onClick, enabled = enabled, content = content)
            }
        }

        null if content != null -> {
            val content: @Composable () -> Unit = { if (showLoading(loading = loading)) CircularProgressIndicator(modifier = Modifier.size(size = 24.dp)) else content() }
            when (style) {
                ButtonStyle.FILLED -> FilledIconButton(modifier = modifier, onClick = { onClick(true) }, enabled = enabled, content = content)
                ButtonStyle.FILLED_TONAL -> FilledTonalIconButton(modifier = modifier, onClick = { onClick(true) }, enabled = enabled, content = content)
                ButtonStyle.ELEVATED -> IconButton(modifier = modifier, onClick = { onClick(true) }, enabled = enabled, content = content)
                ButtonStyle.OUTLINED -> OutlinedIconButton(modifier = modifier, onClick = { onClick(true) }, enabled = enabled, content = content)
                ButtonStyle.TEXT -> IconButton(modifier = modifier, onClick = { onClick(true) }, enabled = enabled, content = content)
            }
        }

        else -> {
            val content: @Composable RowScope.() -> Unit = {
                if (showLoading(loading = loading)) CircularProgressIndicator(modifier = Modifier.size(size = 24.dp)) else {
                    if (content != null) {
                        content()
                        Spacer(modifier = Modifier.width(width = 8.dp))
                    }
                    Text(
                        text = text.orEmpty(),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        style = typography.labelLarge
                    )
                }
            }
            when (style) {
                ButtonStyle.FILLED -> MaterialButton(modifier = modifier, onClick = { onClick(true) }, enabled = isEnabled, content = content)
                ButtonStyle.FILLED_TONAL -> FilledTonalButton(modifier = modifier, onClick = { onClick(true) }, enabled = isEnabled, content = content)
                ButtonStyle.ELEVATED -> ElevatedButton(modifier = modifier, onClick = { onClick(true) }, enabled = isEnabled, content = content)
                ButtonStyle.OUTLINED -> OutlinedButton(modifier = modifier, onClick = { onClick(true) }, enabled = isEnabled, content = content)
                ButtonStyle.TEXT -> TextButton(modifier = modifier, onClick = { onClick(true) }, enabled = isEnabled, content = content)
            }
        }
    }
}

// TODO - SplitButton

@Preview
@Composable
private fun ButtonPreview() = Preview {
    Row(horizontalArrangement = Arrangement.spacedBy(space = 24.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
            Button(style = ButtonStyle.FILLED, text = "Filled")
            Button(style = ButtonStyle.FILLED_TONAL, text = "Filled tonal")
            Button(style = ButtonStyle.ELEVATED, text = "Elevated")
            Button(style = ButtonStyle.OUTLINED, text = "Outlined")
            Button(style = ButtonStyle.TEXT, text = "Text")
        }
        Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
            Button(style = ButtonStyle.FILLED, text = "Filled", enabled = false)
            Button(style = ButtonStyle.FILLED_TONAL, text = "Filled tonal", enabled = false)
            Button(style = ButtonStyle.ELEVATED, text = "Elevated", enabled = false)
            Button(style = ButtonStyle.OUTLINED, text = "Outlined", enabled = false)
            Button(style = ButtonStyle.TEXT, text = "Text", enabled = false)
        }
        Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
            Button(style = ButtonStyle.FILLED, text = "Filled", content = { Icon(imageVector = Icons.Default.Apps) })
            Button(style = ButtonStyle.FILLED_TONAL, text = "Filled tonal", content = { Icon(imageVector = Icons.Default.Apps) })
            Button(style = ButtonStyle.ELEVATED, text = "Elevated", content = { Icon(imageVector = Icons.Default.Apps) })
            Button(style = ButtonStyle.OUTLINED, text = "Outlined", content = { Icon(imageVector = Icons.Default.Apps) })
            Button(style = ButtonStyle.TEXT, text = "Text", content = { Icon(imageVector = Icons.Default.Apps) })
        }
        Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
            Button(style = ButtonStyle.FILLED, text = "Filled", enabled = false, content = { Icon(imageVector = Icons.Default.Apps) })
            Button(style = ButtonStyle.FILLED_TONAL, text = "Filled tonal", enabled = false, content = { Icon(imageVector = Icons.Default.Apps) })
            Button(style = ButtonStyle.ELEVATED, text = "Elevated", enabled = false, content = { Icon(imageVector = Icons.Default.Apps) })
            Button(style = ButtonStyle.OUTLINED, text = "Outlined", enabled = false, content = { Icon(imageVector = Icons.Default.Apps) })
            Button(style = ButtonStyle.TEXT, text = "Text", enabled = false, content = { Icon(imageVector = Icons.Default.Apps) })
        }
        Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
            Button()
            Button(loading = true, text = "Button")
            Button(loading = true, enabled = false, text = "Button")
            var loading by remember { mutableStateOf(value = false) }
            LaunchedEffect(key1 = loading) {
                if (loading) {
                    delay(timeMillis = 2000)
                    loading = false
                }
            }
            Button(loading = loading, enabled = true, text = "Click me!", onClick = { loading = true })
        }
    }
}

@Preview
@Composable
private fun IconButtonPreview() = Preview {
    Row(horizontalArrangement = Arrangement.spacedBy(space = 24.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
            ButtonStyle.entries.forEach { style ->
                Button(style = style, content = { Icon(imageVector = Icons.Default.Apps) })
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
            ButtonStyle.entries.forEach { style ->
                Button(style = style, enabled = false, content = { Icon(imageVector = Icons.Default.Apps) })
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
            Button(loading = true, content = { Icon(imageVector = Icons.Default.Apps) })
            Button(loading = true, enabled = false, content = { Icon(imageVector = Icons.Default.Apps) })
            var loading by remember { mutableStateOf(value = false) }
            LaunchedEffect(key1 = loading) {
                if (loading) {
                    delay(timeMillis = 2000)
                    loading = false
                }
            }
            Button(loading = loading, enabled = true, onClick = { loading = true }, content = { Icon(imageVector = Icons.Default.Apps) })
        }
    }
}

@Preview
@Composable
private fun ToggleButtonPreview() = Preview {
    Row(horizontalArrangement = Arrangement.spacedBy(space = 24.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
            ButtonStyle.entries.forEach { style ->
                var checked by remember { mutableStateOf(value = true) }
                Button(style = style, checked = checked, onClick = { checked = it }, content = { Icon(imageVector = Icons.Default.Apps) })
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
            ButtonStyle.entries.forEach { style ->
                Button(style = style, checked = true, enabled = false, content = { Icon(imageVector = Icons.Default.Apps) })
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
            ButtonStyle.entries.forEach { style ->
                Button(style = style, checked = false, enabled = false, content = { Icon(imageVector = Icons.Default.Apps) })
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
            Button(loading = true, checked = true, content = { Icon(imageVector = Icons.Default.Apps) })
            Button(loading = true, checked = true, enabled = false, content = { Icon(imageVector = Icons.Default.Apps) })
            var loading by remember { mutableStateOf(value = false) }
            LaunchedEffect(key1 = loading) {
                if (loading) {
                    delay(timeMillis = 2000)
                    loading = false
                }
            }
            Button(loading = loading, checked = true, enabled = true, onClick = { loading = true }, content = { Icon(imageVector = Icons.Default.Apps) })
        }
    }
}
