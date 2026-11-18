package com.app.builder.ui.component.field

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
import com.app.builder.ui.Preview
import com.app.builder.ui.core.button.Button
import com.app.builder.ui.core.button.ButtonStyle
import com.app.builder.ui.core.image.Icon
import com.app.builder.ui.core.text.Input

/**
 * A single line password input, masked until its trailing button is clicked, which shows the password and turns into a button that hides it again.
 *
 * @param modifier The [Modifier] to be applied to the input.
 * @param enabled Whether the password can be changed.
 * @param value The password.
 * @param onValueChange Callback for when the password changes.
 * @param label An optional label, translated.
 * @param isError Whether the password is invalid.
 */
@Composable
fun PasswordField(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    value: String = "",
    onValueChange: (String) -> Unit = {},
    label: String? = null,
    isError: Boolean = false,
) {
    var visible by remember { mutableStateOf(value = false) }

    Input(
        modifier = modifier,
        enabled = enabled,
        value = value,
        onValueChange = onValueChange,
        label = label,
        maxLines = 1,
        keyboardType = KeyboardType.Password,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            Button(
                style = ButtonStyle.TEXT,
                content = {
                    Icon(
                        imageVector = if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (visible) "hide_password" else "show_password"
                    )
                },
                onClick = { visible = !visible }
            )
        },
        isError = isError
    )
}

@PreviewLightDark
@Composable
private fun PasswordFieldPreview() = Preview {
    var value by remember { mutableStateOf(value = "secret") }
    PasswordField(label = "Password", value = value, onValueChange = { value = it })
}
