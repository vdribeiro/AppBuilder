package com.app.builder.ui.component.container

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.app.builder.ui.InjectTranslations
import com.app.builder.ui.LocalTypography
import com.app.builder.ui.Preview
import com.app.builder.ui.core.button.Button
import com.app.builder.ui.core.text.Input
import com.app.builder.ui.core.text.Text

/**
 * A login form.
 *
 * @param modifier The [Modifier] to be applied to the root layout.
 * @param loading Whether the login is currently loading.
 * @param username The current username.
 * @param onUsernameValueChange Callback invoked with the edited username when the user makes a change.
 * @param password The current password.
 * @param onPasswordValueChange Callback invoked with the edited password when the user makes a change.
 * @param onSubmit Callback invoked when the user submits the form.
 */
@Composable
fun Login(
    modifier: Modifier = Modifier,
    loading: Boolean = true,
    username: String = "",
    onUsernameValueChange: (String) -> Unit = {},
    password: String = "",
    onPasswordValueChange: (String) -> Unit = {},
    onSubmit: () -> Unit = {},
) {
    val typography = LocalTypography.current

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(space = 16.dp)
    ) {
        Text(
            text = "app_name",
            style = typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(height = 32.dp))

        // Username Input
        Column(verticalArrangement = Arrangement.spacedBy(space = 4.dp)) {
            Text(text = "username", style = typography.labelMedium)
            Input(
                modifier = Modifier
                    .testTag(tag = "username")
                    .fillMaxWidth(),
                value = username,
                onValueChange = onUsernameValueChange,
                enabled = !loading,
                maxLines = 1,
            )
        }

        // Password Input
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = "password", style = typography.labelMedium)
            Input(
                modifier = Modifier
                    .testTag(tag = "password")
                    .fillMaxWidth(),
                value = password,
                onValueChange = onPasswordValueChange,
                enabled = !loading,
                maxLines = 1,
            )
        }

        Spacer(modifier = Modifier.height(height = 16.dp))

        // Submit Button
        Button(
            modifier = Modifier
                .testTag(tag = "login")
                .fillMaxWidth(),
            text = "login",
            loading = loading,
            enabled = !loading && username.isNotBlank() && password.isNotBlank(),
            onClick = { onSubmit() }
        )
    }
}

@Preview
@Composable
private fun LoginPreview() = Preview {
    InjectTranslations(
        translations = mapOf(
            "app_name" to "App Builder",
            "username" to "Username",
            "password" to "Password",
            "login" to "Login"
        )
    )
    Login(
        loading = true,
        username = "test@example.com",
        password = "password123"
    )
}
