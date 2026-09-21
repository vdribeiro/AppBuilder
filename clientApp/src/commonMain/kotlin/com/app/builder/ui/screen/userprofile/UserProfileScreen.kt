package com.app.builder.ui.screen.userprofile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.builder.AppInfo
import com.app.builder.ui.Preview
import com.app.builder.ui.component.bar.TopActionBar
import com.app.builder.ui.component.container.UserProfile
import com.app.builder.ui.component.navigation.Navigation
import com.app.builder.ui.component.navigation.NavigationState
import com.app.builder.ui.navigation.LocalRouter
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.navigation.open
import com.app.builder.ui.screen.Screen
import com.app.builder.ui.store.Store

/**
 * The User Profile Screen.
 *
 * @param navigationStore Drives the bottom navigation bar.
 * @param store Provides the user profile state and receives its actions
 */
@Composable
fun UserProfileScreen(
    navigationStore: Store<NavigationState, Unit>,
    store: Store<UserProfileScreenState, UserProfileScreenAction>
) {
    val state by store.stateFlow.collectAsStateWithLifecycle()
    val router = LocalRouter.current
    val uri = LocalUriHandler.current

    Screen(
        bottomBar = { Navigation(store = navigationStore) },
        topBar = {
            TopActionBar(
                onBackClick = if (router.backStack.size > 1) {
                    { router.back() }
                } else null
            )
        }
    ) {
        UserProfile(
            guest = state.guest,
            name = state.name,
            image = state.image,
            language = state.language,
            appVersion = state.appVersion,
            onEditProfileClick = { store.send(action = UserProfileScreenAction.EditProfile) },
            onChangePasswordClick = { store.send(action = UserProfileScreenAction.ChangePassword) },
            onNotificationsClick = { store.send(action = UserProfileScreenAction.Notifications) },
            onLogoutClick = { store.send(action = UserProfileScreenAction.Logout) },
            onResetClick = { store.send(action = UserProfileScreenAction.Reset) },
            onSendFeedbackClick = { router.navigate(screen = Screen.Error(error = false)) },
            onCopyrightClick = { uri.open(uri = AppInfo.COPYRIGHT) },
            onPrivacyPolicyClick = { uri.open(uri = AppInfo.PRIVACY_POLICY) },
        )
    }
}

@Preview
@Composable
private fun UserProfileScreenPreview() = Preview {
    UserProfileScreen(
        navigationStore = Store(initialState = NavigationState()),
        store = Store(initialState = UserProfileScreenState()),
    )
}
