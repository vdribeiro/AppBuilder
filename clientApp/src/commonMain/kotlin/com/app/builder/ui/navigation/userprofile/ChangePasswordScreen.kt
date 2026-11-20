package com.app.builder.ui.navigation.userprofile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.builder.ui.InjectTranslations
import com.app.builder.ui.Preview
import com.app.builder.ui.component.bar.TopActionBar
import com.app.builder.ui.component.container.ChangePassword
import com.app.builder.ui.core.overlay.Snackbar
import com.app.builder.ui.getTranslation
import com.app.builder.ui.modifier.onKeyPress
import com.app.builder.ui.navigation.LocalRouter
import com.app.builder.ui.screen.Screen
import com.app.builder.ui.store.Store
import com.app.builder.ui.store.previewStore

/**
 * The user-facing Change Password Screen.
 *
 * @param store Provides the change password screen state and receives its actions.
 */
@Composable
fun ChangePasswordScreen(store: Store<ChangePasswordScreenState, ChangePasswordScreenAction>) {
    val state by store.stateFlow.collectAsStateWithLifecycle()

    val router = LocalRouter.current

    Screen(
        onBackClick = { router.back() },
        topBar = {
            TopActionBar(
                title = "change_password",
                onBackClick = { router.back() }
            )
        },
        snackbarHost = {
            state.message?.let {
                Snackbar(message = getTranslation(key = it), onDismiss = { store.send(action = ChangePasswordScreenAction.DismissMessage) })
            }
        },
        contentAlignment = Alignment.Center
    ) {
        ChangePassword(
            modifier = Modifier.onKeyPress(
                keys = listOf(Key.Enter, Key.NumPadEnter),
                onKey = { store.send(action = ChangePasswordScreenAction.Submit) }
            ),
            loading = state.loading,
            currentPassword = state.currentPassword,
            onCurrentPasswordValueChange = { store.send(action = ChangePasswordScreenAction.UpdateCurrentPassword(password = it)) },
            newPassword = state.newPassword,
            onNewPasswordValueChange = { store.send(action = ChangePasswordScreenAction.UpdateNewPassword(password = it)) },
            confirmPassword = state.confirmPassword,
            onConfirmPasswordValueChange = { store.send(action = ChangePasswordScreenAction.UpdateConfirmPassword(password = it)) },
            onSubmit = { store.send(action = ChangePasswordScreenAction.Submit) }
        )
    }
}

@PreviewLightDark
@Composable
private fun ChangePasswordScreenPreview() = Preview {
    InjectTranslations(
        translations = mapOf(
            "change_password" to "Change Password",
            "current_password" to "Current Password",
            "new_password" to "New Password",
            "confirm_password" to "Confirm Password",
            "unable_to_change_password" to "Unable to change the password"
        )
    )
    ChangePasswordScreen(
        store = previewStore(
            initialState = ChangePasswordScreenState(
                currentPassword = "password123",
                newPassword = "password456",
                confirmPassword = "password456",
                message = "unable_to_change_password"
            )
        )
    )
}
