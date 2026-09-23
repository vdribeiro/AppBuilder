package com.app.builder.ui.component.container

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.app.builder.ui.LocalTypography
import com.app.builder.ui.Preview
import com.app.builder.ui.component.image.Avatar
import com.app.builder.ui.core.card.Card
import com.app.builder.ui.core.divider.Divider
import com.app.builder.ui.core.image.Image
import com.app.builder.ui.core.list.LazyColumn
import com.app.builder.ui.core.list.ListItem
import com.app.builder.ui.core.text.Text

/**
 * A user profile, showing the user's avatar and full name followed by grouped sections of settings.
 *
 * @param modifier The [Modifier] to be applied to the root layout.
 * @param guest true if it is a guest account, false otherwise.
 * @param name The user's full name, shown below the avatar.
 * @param image The user's avatar image.
 * @param language The device language, shown as an informative label.
 * @param appVersion The app version, shown as an informative label.
 * @param onEditProfileClick Callback invoked when edit profile is clicked.
 * @param onChangePasswordClick Callback invoked when change password is clicked.
 * @param onNotificationsClick Callback invoked when notifications is clicked.
 * @param onLogoutClick Callback invoked when logout is clicked.
 * @param onResetClick Callback invoked when reset is clicked.
 * @param onSendFeedbackClick Callback invoked when send feedback is clicked.
 * @param onCopyrightClick Callback invoked when copyright is clicked.
 * @param onPrivacyPolicyClick Callback invoked when privacy policy is clicked.
 */
@Composable
fun UserProfile(
    modifier: Modifier = Modifier,
    guest: Boolean = true,
    name: String? = null,
    image: Image? = null,
    language: String = "",
    appVersion: String = "",
    onEditProfileClick: () -> Unit = {},
    onChangePasswordClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
    onResetClick: () -> Unit = {},
    onSendFeedbackClick: () -> Unit = {},
    onCopyrightClick: () -> Unit = {},
    onPrivacyPolicyClick: () -> Unit = {},
) {
    val typography = LocalTypography.current

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(space = 24.dp),
    ) {
        if (!guest) item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(space = 8.dp),
            ) {
                Avatar(name = name, image = image, size = 96.dp, contentDescription = name)
                Text(text = name.orEmpty(), style = typography.titleLarge)
            }
        }

        if (!guest) item {
            UserProfileSection(title = "account") {
                ListItem(
                    modifier = Modifier.clickable { onEditProfileClick() },
                    headlineText = "edit_profile",
                )
                Divider()
                ListItem(
                    modifier = Modifier.clickable { onChangePasswordClick() },
                    headlineText = "change_password",
                )
                Divider()
                ListItem(
                    modifier = Modifier.clickable { onNotificationsClick() },
                    headlineText = "notifications",
                )
                Divider()
                ListItem(
                    modifier = Modifier.clickable { onLogoutClick() },
                    headlineText = "logout",
                )
            }
        }

        if (!guest) item {
            UserProfileSection(title = "device") {
                ListItem(
                    headlineText = "language",
                    trailingContent = { Text(text = language, style = typography.bodyMedium) },
                )
                Divider()
                ListItem(
                    modifier = Modifier.clickable { onResetClick() },
                    headlineText = "reset",
                )
            }
        }

        item {
            UserProfileSection(title = "about") {
                ListItem(
                    modifier = Modifier.clickable { onSendFeedbackClick() },
                    headlineText = "send_feedback",
                )
                Divider()
                ListItem(
                    modifier = Modifier.clickable { onCopyrightClick() },
                    headlineText = "copyright",
                )
                Divider()
                ListItem(
                    modifier = Modifier.clickable { onPrivacyPolicyClick() },
                    headlineText = "privacy_policy",
                )
                Divider()
                ListItem(
                    headlineText = "app_version",
                    trailingContent = { Text(text = appVersion, style = typography.bodyMedium) },
                )
            }
        }
    }
}

/**
 * A titled group of related settings, rendered as a [Card] of stacked rows.
 *
 * @param modifier The [Modifier] to be applied to the section's layout.
 * @param title The section's title, shown above the card.
 * @param content The section's rows, executing inside the card's sequential [ColumnScope].
 */
@Composable
private fun UserProfileSection(
    modifier: Modifier = Modifier,
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    val typography = LocalTypography.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(space = 8.dp),
    ) {
        Text(text = title, style = typography.titleMedium)
        Card(content = content)
    }
}

@Preview
@Composable
private fun UserProfilePreview() = Preview {
    UserProfile(
        name = "Vitor Ribeiro",
        language = "Ingrish",
        appVersion = "1.0.0",
    )
}
