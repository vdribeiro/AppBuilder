package com.app.builder.ui.component.container

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.PreviewLightDark
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
import com.app.builder.ui.getTranslation

/**
 * A user profile, showing the user's avatar and full name followed by grouped sections of settings.
 *
 * @param modifier The [Modifier] to be applied to the root layout.
 * @param guest true if it is a guest account, false otherwise.
 * @param name The user's full name, shown below the avatar.
 * @param image The user's avatar image.
 * @param language The ISO code of the language the app is shown in, shown translated.
 * @param onLanguageClick Callback invoked when language is clicked, or `null` for a language row that can't be clicked.
 * @param appVersion The app version, shown as an informative label.
 * @param onEditProfileClick Callback invoked when edit profile is clicked.
 * @param onChangePasswordClick Callback invoked when change password is clicked.
 * @param onNotificationsClick Callback invoked when notifications is clicked, or `null` to hide notifications.
 * @param onLogoutClick Callback invoked when logout is clicked.
 * @param onResetClick Callback invoked when reset is clicked.
 * @param onHelpClick Callback invoked when help is clicked, or `null` to hide help.
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
    onLanguageClick: (() -> Unit)? = null,
    appVersion: String = "",
    onEditProfileClick: () -> Unit = {},
    onChangePasswordClick: () -> Unit = {},
    onNotificationsClick: (() -> Unit)? = null,
    onLogoutClick: () -> Unit = {},
    onResetClick: () -> Unit = {},
    onHelpClick: (() -> Unit)? = null,
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
            UserProfileSection(title = "profile_account") {
                ListItem(
                    modifier = Modifier.clickable { onEditProfileClick() },
                    headlineText = getTranslation(key = "profile_edit_profile"),
                )
                Divider()
                ListItem(
                    modifier = Modifier.clickable { onChangePasswordClick() },
                    headlineText = getTranslation(key = "profile_change_password"),
                )
                Divider()
                if (onNotificationsClick != null) {
                    ListItem(
                        modifier = Modifier.clickable { onNotificationsClick() },
                        headlineText = getTranslation(key = "profile_notifications"),
                    )
                    Divider()
                }
                ListItem(
                    modifier = Modifier.clickable { onLogoutClick() },
                    headlineText = getTranslation(key = "profile_logout"),
                )
            }
        }

        item {
            UserProfileSection(title = "profile_device") {
                ListItem(
                    modifier = Modifier
                        .testTag(tag = "language")
                        .then(other = onLanguageClick?.let { Modifier.clickable { it() } } ?: Modifier),
                    headlineText = getTranslation(key = "profile_language"),
                    trailingContent = { Text(text = getTranslation(key = "profile_language_$language", default = language), style = typography.bodyMedium) },
                )
                if (!guest) {
                    Divider()
                    ListItem(
                        modifier = Modifier.clickable { onResetClick() },
                        headlineText = getTranslation(key = "profile_reset"),
                    )
                }
            }
        }

        item {
            UserProfileSection(title = "profile_about") {
                if (onHelpClick != null) {
                    ListItem(
                        modifier = Modifier.clickable { onHelpClick() },
                        headlineText = getTranslation(key = "profile_help"),
                    )
                    Divider()
                }
                ListItem(
                    modifier = Modifier.clickable { onSendFeedbackClick() },
                    headlineText = getTranslation(key = "profile_send_feedback"),
                )
                Divider()
                ListItem(
                    modifier = Modifier.clickable { onCopyrightClick() },
                    headlineText = getTranslation(key = "profile_copyright"),
                )
                Divider()
                ListItem(
                    modifier = Modifier.clickable { onPrivacyPolicyClick() },
                    headlineText = getTranslation(key = "profile_privacy_policy"),
                )
                Divider()
                ListItem(
                    headlineText = getTranslation(key = "profile_app_version"),
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
 * @param title The section's title, translated, shown above the card.
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
        Text(text = getTranslation(key = title), style = typography.titleMedium)
        Card(content = content)
    }
}

@PreviewLightDark
@Composable
private fun UserProfilePreview() = Preview {
    UserProfile(
        name = "Vitor Ribeiro",
        language = "en",
        appVersion = "1.0.0",
        onHelpClick = {},
    )
}
