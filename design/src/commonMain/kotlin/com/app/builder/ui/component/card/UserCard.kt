package com.app.builder.ui.component.card

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.app.builder.ui.InjectTranslations
import com.app.builder.ui.LocalTypography
import com.app.builder.ui.Preview
import com.app.builder.ui.component.button.PermissionItem
import com.app.builder.ui.component.button.PermissionSelector
import com.app.builder.ui.component.image.Avatar
import com.app.builder.ui.component.text.InputField
import com.app.builder.ui.core.button.Switch
import com.app.builder.ui.core.card.Card
import com.app.builder.ui.core.image.Image
import com.app.builder.ui.core.text.Text

/**
 * A user card.
 *
 * @param modifier The [Modifier] to be applied to the root layout.
 * @param enabled Controls whether the user fields can accept user focus, selection gestures, or keystroke input updates.
 * @param selected The visual emphasis state of the card.
 * @param uuid The UUID of the user.
 * @param modifiedAt The modification timestamp of the user.
 * @param deletedAt The deletion timestamp of the user.
 * @param name The name of the user.
 * @param onNameChange Callback for when the name changes.
 * @param avatar The avatar image of the user, falling back to the initials of [name].
 * @param accountEnabled Controls whether the account fields can accept user focus, selection gestures, or keystroke input updates.
 * @param username The username of the user's account, or `null` to hide it.
 * @param onUsernameChange Callback for when the username changes.
 * @param password The password of a new account, or `null` to hide it.
 * @param onPasswordChange Callback for when the password changes.
 * @param accountActive Whether the user's account is active, or `null` to hide it.
 * @param onAccountActiveChange Callback for when the account is activated or disabled.
 * @param permissions The permissions of the user's account, or `null` to hide them.
 * @param onPermissionToggled Callback for when a permission is toggled.
 */
@Composable
fun UserCard(
    modifier: Modifier = Modifier,
    enabled: Boolean = false,
    selected: Boolean = false,
    uuid: String? = null,
    modifiedAt: String? = null,
    deletedAt: String? = null,
    name: String? = null,
    onNameChange: (String) -> Unit = {},
    avatar: Image? = null,
    accountEnabled: Boolean = false,
    username: String? = null,
    onUsernameChange: (String) -> Unit = {},
    password: String? = null,
    onPasswordChange: (String) -> Unit = {},
    accountActive: Boolean? = null,
    onAccountActiveChange: (Boolean) -> Unit = {},
    permissions: ImmutableList<PermissionItem>? = null,
    onPermissionToggled: (PermissionItem) -> Unit = {},
) {
    val typography = LocalTypography.current
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(all = 8.dp),
        selected = selected
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = 8.dp),
            verticalArrangement = Arrangement.spacedBy(space = 8.dp, alignment = Alignment.Top),
            horizontalAlignment = Alignment.Start,
        ) {
            Avatar(name = name, image = avatar, contentDescription = name)
            uuid?.let {
                InputField(
                    enabled = false,
                    title = "user_uuid",
                    value = it,
                )
            }
            name?.let {
                InputField(
                    enabled = enabled,
                    title = "user_name",
                    value = it,
                    onValueChange = onNameChange
                )
            }
            if (username != null || password != null || accountActive != null || permissions != null) {
                Text(
                    modifier = Modifier.padding(all = 8.dp),
                    text = "account",
                    style = typography.titleLarge
                )
            }
            username?.let {
                InputField(
                    enabled = accountEnabled,
                    title = "username",
                    value = it,
                    onValueChange = onUsernameChange
                )
            }
            password?.let {
                InputField(
                    enabled = accountEnabled,
                    title = "password",
                    value = it,
                    onValueChange = onPasswordChange
                )
            }
            accountActive?.let {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(all = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        modifier = Modifier.weight(weight = 1f),
                        text = "account_active",
                        style = typography.titleLarge
                    )
                    Switch(
                        modifier = Modifier.testTag(tag = "account_active"),
                        enabled = accountEnabled,
                        checked = it,
                        onCheckedChange = onAccountActiveChange
                    )
                }
            }
            permissions?.let {
                PermissionSelector(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(all = 8.dp),
                    enabled = accountEnabled,
                    permissions = it,
                    onPermissionToggled = onPermissionToggled
                )
            }
            modifiedAt?.let {
                InputField(
                    enabled = false,
                    title = "user_modified_at",
                    value = it,
                )
            }
            deletedAt?.let {
                InputField(
                    enabled = false,
                    title = "user_deleted_at",
                    value = it,
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun UserCardPreview() = Preview {
    InjectTranslations(
        translations = mapOf(
            "user_name" to "Name",
            "user_modified_at" to "Modified At",
            "username" to "Username",
            "password" to "Password",
            "account" to "Account",
            "account_active" to "Active",
            "permissions" to "Permissions",
            "read" to "Read",
            "write" to "Write",
        )
    )
    Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
        UserCard(
            enabled = false,
            selected = false,
            name = "Vitor Ribeiro",
            modifiedAt = "2026-11-04T10:00:00Z"
        )
        UserCard(
            enabled = true,
            selected = true,
            accountEnabled = true,
            name = "Vitor Ribeiro",
            username = "vribeiro",
            password = "",
            accountActive = true,
            permissions = persistentListOf(
                PermissionItem(id = "TASK", name = "Task", read = true, write = true),
                PermissionItem(id = "USER", name = "User", read = true),
            )
        )
    }
}
