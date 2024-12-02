package com.app.builder.ui.component.text

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.app.builder.ui.LocalTypography
import com.app.builder.ui.Preview
import com.app.builder.ui.core.image.Icon
import com.app.builder.ui.core.text.Input
import com.app.builder.ui.core.text.Text

/**
 * A field with a text and input.
 *
 * @param modifier The [Modifier] to be applied to the root layout.
 * @param enabled Controls whether the input can accept user focus, selection gestures, or keystroke input updates.
 * @param title The title of the input.
 * @param value The text buffer string value currently displayed inside the layout window.
 * @param onValueChange Callback dispatched instantly on every localized keyboard input stroke update.
 * @param leadingIcon An optional composable slot rendering decorative action widgets or iconography assets at the front boundary of the input field.
 * @param isError Controls whether the input is in an error state.
 */
@Composable
fun InputField(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    title: String = "",
    value: String = "",
    onValueChange: (String) -> Unit = {},
    leadingIcon: (@Composable () -> Unit)? = null,
    isError: Boolean = false
) {
    val typography = LocalTypography.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(all = 8.dp),
        verticalArrangement = Arrangement.spacedBy(space = 8.dp, alignment = Alignment.Top),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(text = title, style = typography.titleLarge)
        Input(
            value = value,
            enabled = enabled,
            onValueChange = onValueChange,
            maxLines = 1,
            leadingIcon = leadingIcon,
            isError = isError
        )
    }
}

@Preview
@Composable
private fun InputFieldPreview() = Preview {
    var value by remember { mutableStateOf(value = "") }
    Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
        InputField(
            title = "Title",
            enabled = true,
            value = value,
            onValueChange = { value = it },
            leadingIcon = { Icon(imageVector = Icons.Default.Email) }
        )
        InputField(
            title = "Title",
            enabled = false,
            value = value,
            onValueChange = { value = it },
        )
        InputField(
            title = "Title",
            isError = true,
            value = value,
            onValueChange = { value = it },
        )
    }
}
