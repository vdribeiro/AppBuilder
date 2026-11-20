package com.app.builder.ui.component.field

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import com.app.builder.ui.core.button.Button
import com.app.builder.ui.core.button.ButtonStyle
import com.app.builder.ui.core.image.Icon
import com.app.builder.ui.core.text.Input
import com.app.builder.ui.core.text.TextValue

/**
 * A text property: an outlined input with a floating label while [editing], and a [FieldValue] otherwise.
 *
 * @param modifier The [Modifier] to be applied to the root layout.
 * @param label The label of the field, translated, or `null` for a field without one.
 * @param value The text value.
 * @param editing Whether the value can be changed.
 * @param enabled Whether the input accepts changes while editing.
 * @param onValueChange Callback for when the value changes.
 * @param keyboardType The type of software keyboard requested while editing, such as text, number, email or password.
 * @param maxLines The maximum number of lines shown while editing before the input scrolls. With `1`, the input is single line. When not editing, the value is always shown in full.
 * @param isError Whether the value is invalid.
 */
@Composable
fun TextField(
    modifier: Modifier = Modifier,
    label: String? = null,
    value: String = "",
    editing: Boolean = false,
    enabled: Boolean = true,
    onValueChange: (String) -> Unit = {},
    keyboardType: KeyboardType = KeyboardType.Text,
    maxLines: Int = 1,
    isError: Boolean = false,
) {
    val password = keyboardType == KeyboardType.Password
    var passwordVisible by remember { mutableStateOf(value = false) }

    if (editing) Input(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        enabled = enabled,
        value = value,
        onValueChange = onValueChange,
        label = label,
        maxLines = maxLines,
        keyboardType = keyboardType,
        visualTransformation = if (password && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        trailingIcon = if (password) {
            {
                Button(
                    style = ButtonStyle.TEXT,
                    content = {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (passwordVisible) "hide_password" else "show_password"
                        )
                    },
                    onClick = { passwordVisible = !passwordVisible }
                )
            }
        } else null,
        isError = isError
    ) else FieldValue(
        modifier = modifier,
        label = label.orEmpty(),
        value = (if (password) PASSWORD_MASK.takeIf { value.isNotEmpty() } else value)?.let { TextValue.Plain(text = it) }
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
        TextField(label = "Password", value = "secret", editing = true, keyboardType = KeyboardType.Password)
        TextField(label = "Email", value = "", editing = true, keyboardType = KeyboardType.Email, isError = true)
    }
}
