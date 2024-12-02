package com.app.builder.ui.component.container

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.app.builder.ui.InjectTranslations
import com.app.builder.ui.LocalTypography
import com.app.builder.ui.Preview
import com.app.builder.ui.component.button.PermissionItem
import com.app.builder.ui.component.button.PermissionSelector
import com.app.builder.ui.core.button.Button
import com.app.builder.ui.core.image.Icon
import com.app.builder.ui.core.text.Input
import com.app.builder.ui.core.text.Text

/**
 * A registration form.
 *
 * @param loading Whether the registration is currently loading.
 * @param name The current name.
 * @param onNameValueChange Callback invoked with the edited name when the user makes a change.
 * @param username The current username.
 * @param onUsernameValueChange Callback invoked with the edited username when the user makes a change.
 * @param password The current password.
 * @param onPasswordValueChange Callback invoked with the edited password when the user makes a change.
 * @param permissions The permissions granted to the new account, one [PermissionItem] per entity.
 * @param onPermissionToggled Callback invoked with the edited permission when the user makes a change.
 * @param onSubmit Callback invoked when the user submits the form.
 */
@Composable
fun RegistrationForm(
    loading: Boolean = true,
    name: String = "",
    onNameValueChange: (String) -> Unit = {},
    username: String = "",
    onUsernameValueChange: (String) -> Unit = {},
    password: String = "",
    onPasswordValueChange: (String) -> Unit = {},
    permissions: ImmutableList<PermissionItem> = persistentListOf(),
    onPermissionToggled: (PermissionItem) -> Unit = {},
    onSubmit: () -> Unit = {},
) {
    val typography = LocalTypography.current

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(space = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(weight = 1f)
                .verticalScroll(state = rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(space = 16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(space = 4.dp)) {
                Text(text = "name", style = typography.labelMedium)
                Input(
                    modifier = Modifier
                        .testTag(tag = "name")
                        .fillMaxWidth(),
                    value = name,
                    onValueChange = onNameValueChange,
                    enabled = !loading,
                    maxLines = 1,
                    leadingIcon = { Icon(imageVector = Icons.Default.Person) }
                )
            }

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
                    leadingIcon = { Icon(imageVector = Icons.Default.Email) }
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(space = 4.dp)) {
                Text(text = "password", style = typography.labelMedium)
                Input(
                    modifier = Modifier
                        .testTag(tag = "password")
                        .fillMaxWidth(),
                    value = password,
                    onValueChange = onPasswordValueChange,
                    enabled = !loading,
                    maxLines = 1,
                    leadingIcon = { Icon(imageVector = Icons.Default.Lock) }
                )
            }

            PermissionSelector(
                modifier = Modifier.fillMaxWidth(),
                enabled = !loading,
                permissions = permissions,
                onPermissionToggled = onPermissionToggled
            )
        }

        Button(
            modifier = Modifier
                .testTag(tag = "register")
                .fillMaxWidth(),
            text = "register",
            loading = loading,
            enabled = name.isNotBlank() && username.isNotBlank() && password.isNotBlank(),
            onClick = { onSubmit() }
        )
    }
}

@Preview
@Composable
private fun RegistrationFormPreview() = Preview {
    InjectTranslations(
        translations = mapOf(
            "app_name" to "App Builder",
            "name" to "Name",
            "username" to "Username",
            "password" to "Password",
            "permissions" to "Permissions",
            "read" to "Read",
            "write" to "Write",
            "register" to "Register"
        )
    )
    RegistrationForm(
        loading = true,
        name = "Test User",
        username = "test@example.com",
        password = "password123",
        permissions = persistentListOf(
            PermissionItem(id = "CLIENT_FLAG", name = "Client Flag", read = true, write = true),
            PermissionItem(id = "CLIENT_CONFIG", name = "Client Config", read = true),
            PermissionItem(id = "USER", name = "User")
        )
    )
}
