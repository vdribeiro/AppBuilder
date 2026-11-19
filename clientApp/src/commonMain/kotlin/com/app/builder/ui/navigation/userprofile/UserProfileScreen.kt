package com.app.builder.ui.navigation.userprofile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.builder.AppInfo
import com.app.builder.core.config.ClientFlags
import com.app.builder.ui.Preview
import com.app.builder.ui.component.bar.TopActionBar
import com.app.builder.ui.component.container.UserProfile
import com.app.builder.ui.component.overlay.LanguageDialog
import com.app.builder.ui.navigation.LocalRouter
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.navigation.open
import com.app.builder.ui.screen.Screen
import com.app.builder.ui.store.Store
import com.app.builder.ui.store.previewStore

/**
 * The User Profile Screen.
 *
 * @param store Provides the user profile state and receives its actions
 */
@Composable
fun UserProfileScreen(
    store: Store<UserProfileScreenState, UserProfileScreenAction>
) {
    val state by store.stateFlow.collectAsStateWithLifecycle()
    val flags by ClientFlags.flow.collectAsStateWithLifecycle()
    val router = LocalRouter.current
    val uri = LocalUriHandler.current

    Screen(
        onBackClick = { router.back() },
        topBar = {
            TopActionBar(
                onBackClick = if (router.backStack.size > 1) {
                    { router.back() }
                } else null
            )
        },
    ) {
        UserProfile(
            guest = state.guest,
            name = state.name,
            image = state.image,
            language = state.language,
            onLanguageClick = if (state.languages.isNotEmpty()) {
                { store.send(action = UserProfileScreenAction.OpenLanguageDialog) }
            } else null,
            appVersion = state.appVersion,
            onEditProfileClick = { router.navigate(screen = Screen.EditProfile) },
            onChangePasswordClick = { router.navigate(screen = Screen.ChangePassword) },
            onNotificationsClick = if (flags.notifications) {
                { store.send(action = UserProfileScreenAction.Notifications) }
            } else null,
            onLogoutClick = { store.send(action = UserProfileScreenAction.Logout) },
            onResetClick = { store.send(action = UserProfileScreenAction.Reset) },
            onHelpClick = if (flags.help && !state.guest) {
                { router.navigate(screen = Screen.Help) }
            } else null,
            onSendFeedbackClick = { router.navigate(screen = Screen.Error(error = false)) },
            onCopyrightClick = { uri.open(uri = AppInfo.COPYRIGHT) },
            onPrivacyPolicyClick = { uri.open(uri = AppInfo.PRIVACY_POLICY) },
        )
    }

    if (state.languageDialogOpen) LanguageDialog(
        languages = state.languages,
        selectedLanguage = state.pickedLanguage,
        onLanguageSelected = { store.send(action = UserProfileScreenAction.PickLanguage(languageIso = it)) },
        onApply = { store.send(action = UserProfileScreenAction.ApplyLanguage) },
        onDismiss = { store.send(action = UserProfileScreenAction.DismissLanguageDialog) },
    )
}

@PreviewLightDark
@Composable
private fun UserProfileScreenPreview() = Preview {
    UserProfileScreen(
        store = previewStore(initialState = UserProfileScreenState()),
    )
}
