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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.app.builder.ui.InjectTranslations
import com.app.builder.ui.LocalTypography
import com.app.builder.ui.Preview
import com.app.builder.ui.component.button.PermissionItem
import com.app.builder.ui.component.button.PermissionSelector
import com.app.builder.ui.component.field.BooleanField
import com.app.builder.ui.component.field.MetadataSection
import com.app.builder.ui.component.field.TextField
import com.app.builder.ui.component.image.Avatar
import com.app.builder.ui.core.card.Card
import com.app.builder.ui.core.image.Image
import com.app.builder.ui.core.text.Text

/**
 * A user card, with the user's avatar and name on top, followed by their account. Only the properties that are not `null` are shown.
 *
 * @param modifier The [Modifier] to be applied to the root layout.
 * @param enabled Whether the user's properties can be edited.
 * @param uuid The UUID of the user.
 * @param modifiedAt The modification timestamp of the user.
 * @param deletedAt The deletion timestamp of the user.
 * @param name The name of the user.
 * @param onNameChange Callback for when the name changes.
 * @param avatar The avatar image of the user, falling back to the initials of [name].
 * @param accountEnabled Whether the account's properties can be edited.
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
            .padding(all = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
        ) {
            Row(
                modifier = Modifier.padding(start = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Avatar(name = name, image = avatar, contentDescription = name, size = 56.dp)
                name?.let {
                    TextField(
                        modifier = Modifier.weight(weight = 1f),
                        label = "user_name",
                        value = it,
                        editing = enabled,
                        onValueChange = onNameChange
                    )
                }
            }
            if (username != null || password != null || accountActive != null || permissions != null) {
                Text(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
                    text = "account",
                    style = typography.titleSmall
                )
            }
            username?.let {
                TextField(
                    label = "username",
                    value = it,
                    editing = accountEnabled,
                    onValueChange = onUsernameChange
                )
            }
            password?.let {
                TextField(
                    label = "password",
                    value = it,
                    editing = accountEnabled,
                    onValueChange = onPasswordChange,
                    keyboardType = KeyboardType.Password
                )
            }
            accountActive?.let {
                BooleanField(
                    modifier = Modifier.testTag(tag = "account_active"),
                    label = "account_active",
                    checked = it,
                    editing = accountEnabled,
                    onCheckedChange = onAccountActiveChange
                )
            }
            permissions?.let {
                PermissionSelector(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    enabled = accountEnabled,
                    permissions = it,
                    onPermissionToggled = onPermissionToggled
                )
            }
            MetadataSection(
                entries = persistentListOf(
                    "user_uuid" to uuid,
                    "user_modified_at" to modifiedAt,
                    "user_deleted_at" to deletedAt,
                )
            )
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
            name = "Vitor Ribeiro",
            username = "vribeiro",
            accountActive = true,
            modifiedAt = "2026-11-04T10:00:00Z"
        )
        UserCard(
            enabled = true,
            accountEnabled = true,
            name = "Vitor Ribeiro",
            username = "vribeiro",
            password = "",
            permissions = persistentListOf(
                PermissionItem(id = "TASK", name = "Task", read = true, write = true),
                PermissionItem(id = "USER", name = "User", read = true),
            )
        )
    }
}
