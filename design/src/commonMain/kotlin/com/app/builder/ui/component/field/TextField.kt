package com.app.builder.ui.component.field

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.app.builder.ui.Preview
import com.app.builder.ui.core.text.Input

/**
 * A text property: an outlined input with a floating label while [editing], and a [FieldValue] otherwise.
 *
 * @param modifier The [Modifier] to be applied to the root layout.
 * @param label The label of the field, translated.
 * @param value The text value.
 * @param editing Whether the value can be changed.
 * @param onValueChange Callback for when the value changes.
 * @param keyboardType The type of software keyboard requested while editing, such as text, number, email or password.
 * @param maxLines The maximum number of lines shown while editing before the input scrolls. With `1`, the input is single line. When not editing, the value is always shown in full.
 * @param isError Whether the value is invalid.
 */
@Composable
fun TextField(
    modifier: Modifier = Modifier,
    label: String = "",
    value: String = "",
    editing: Boolean = false,
    onValueChange: (String) -> Unit = {},
    keyboardType: KeyboardType = KeyboardType.Text,
    maxLines: Int = 1,
    isError: Boolean = false,
) {
    val password = keyboardType == KeyboardType.Password

    if (editing) Input(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        value = value,
        onValueChange = onValueChange,
        label = label,
        maxLines = maxLines,
        keyboardType = keyboardType,
        visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
        isError = isError
    ) else FieldValue(
        modifier = modifier,
        label = label,
        value = if (password) PASSWORD_MASK.takeIf { value.isNotEmpty() } else value
    )
}

/** Shown in place of a password that is not being edited. */
private const val PASSWORD_MASK = "••••••••"

@PreviewLightDark
@Composable
private fun TextFieldPreview() = Preview {
    var value by remember { mutableStateOf(value = "Replace the lobby lights") }
    Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
        TextField(label = "Title", value = value)
        TextField(label = "Title", value = value, editing = true, onValueChange = { value = it })
        TextField(label = "Password", value = "secret", keyboardType = KeyboardType.Password)
        TextField(label = "Email", value = "", editing = true, keyboardType = KeyboardType.Email, isError = true)
    }
}
