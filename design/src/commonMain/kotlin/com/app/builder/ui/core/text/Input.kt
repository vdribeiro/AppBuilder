package com.app.builder.ui.core.text

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.app.builder.ui.Preview
import com.app.builder.ui.core.image.Icon

/**
 * Text entry component that standardizes the visual representation of input fields across the user interface, supporting fully customizable styling configurations,
 * maximum horizontal line layouts, and slot-based auxiliary decorations like leading search or filter icons.
 *
 * @param modifier The [Modifier] to be applied to the bounding layout structure of the text input.
 * @param enabled Controls whether the input can accept user focus, selection gestures, or keystroke input updates.
 * @param value The text buffer string value currently displayed inside the layout window.
 * @param onValueChange Callback dispatched instantly on every localized keyboard input stroke update.
 * @param maxLines The maximum vertical span limit allocation constraints before wrapping layout text blocks into scrolling viewports.
 * @param style Typography styling applied to the text field content. Defaults to [LocalTextStyle].
 * @param leadingIcon An optional composable slot rendering decorative action widgets or iconography assets at the front boundary of the input field.
 * @param isError Controls whether the input is in an error state.
 */
@Composable
fun Input(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    value: String = "",
    onValueChange: (String) -> Unit = {},
    maxLines: Int = Int.MAX_VALUE,
    style: TextStyle = LocalTextStyle.current,
    leadingIcon: (@Composable () -> Unit)? = null,
    isError: Boolean = false
) {
    OutlinedTextField(
        modifier = modifier,
        enabled = enabled,
        value = value,
        onValueChange = onValueChange,
        maxLines = maxLines,
        textStyle = style,
        leadingIcon = leadingIcon,
        isError = isError
    )
}

@Preview
@Composable
private fun InputPreview() = Preview {
    var value by remember { mutableStateOf(value = "") }
    Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
        Input(
            enabled = true,
            value = value,
            onValueChange = { value = it },
            leadingIcon = { Icon(imageVector = Icons.Default.Search) }
        )
        Input(
            enabled = true,
            value = value,
            onValueChange = { value = it },
            maxLines = 10
        )
        Input(
            enabled = false,
            value = value,
            onValueChange = { value = it },
        )
        Input(
            isError = true,
            value = value,
            onValueChange = { value = it },
        )
        Input(
            enabled = true,
            value = "A very very long text in this compose preview",
            maxLines = 1
        )
    }
}