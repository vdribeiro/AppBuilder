package com.app.builder.ui.navigation.userprofile

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Job
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.domain.PasswordFailure
import com.app.builder.domain.gateway.authentication.AuthenticationUseCases
import com.app.builder.domain.gateway.authentication.PasswordResult
import com.app.builder.ui.navigation.Router
import com.app.builder.ui.store.Store

/**
 * Store backing the Change Password Screen, observing the current user and handling password updates and the password change submission.
 *
 * @param state Initial Change Password Screen state.
 * @property router The router used for navigation.
 * @property authenticationUseCases Use cases used to observe the current user and change its password.
 */
class ChangePasswordScreenStore(
    state: ChangePasswordScreenState,
    private val router: Router,
    private val authenticationUseCases: AuthenticationUseCases
): Store<ChangePasswordScreenState, ChangePasswordScreenAction>(initialState = state) {
    init {
        setup()
    }

    override fun reducer(state: ChangePasswordScreenState, action: ChangePasswordScreenAction) {
        super.reducer(state = state, action = action)
        when (action) {
            is ChangePasswordScreenAction.UpdateCurrentPassword -> updateState { it.copy(currentPassword = action.password) }
            is ChangePasswordScreenAction.UpdateNewPassword -> updateState { it.copy(newPassword = action.password) }
            is ChangePasswordScreenAction.UpdateConfirmPassword -> updateState { it.copy(confirmPassword = action.password) }
            ChangePasswordScreenAction.Submit -> submit(state = state)
            ChangePasswordScreenAction.DismissMessage -> updateState { it.copy(message = null, messageArgs = persistentListOf()) }
        }
    }

    /**
     * Observes the current user.
     *
     * @return The [Job] representing this execution.
     */
    private fun setup(): Job = launch(id = "setup") {
        Telemetry.info(tag = TAG, message = "Setup")

        authenticationUseCases.observeCurrentUser().observe(id = "current_user") { user ->
            updateState { it.copy(userUuid = user?.uuid) }
        }

        Telemetry.info(tag = TAG, message = "Setup complete")
    }

    /**
     * Changes the password of the current user when every password is filled and the confirmation matches the new password, then leaves the screen.
     * The screen stays open with a message telling why when the change fails.
     *
     * @param state Change Password Screen state at the time the submission was requested.
     * @return The [Job] representing this execution.
     */
    private fun submit(state: ChangePasswordScreenState): Job = launch(id = "submit", replace = false) {
        val userUuid = state.userUuid ?: return@launch
        if (state.currentPassword.isBlank() || state.newPassword.isBlank() || state.confirmPassword != state.newPassword) return@launch
        Telemetry.info(tag = TAG, message = "Changing the password of user $userUuid")

        updateState { it.copy(loading = true, message = null, messageArgs = persistentListOf()) }
        val result = authenticationUseCases.changePassword(userUuid = userUuid, currentPassword = state.currentPassword, newPassword = state.newPassword)
        when (result) {
            is PasswordResult.Success -> router.back()
            else -> {
                val (message, messageArgs) = result.toMessage()
                updateState { it.copy(loading = false, message = message, messageArgs = messageArgs) }
            }
        }

        Telemetry.info(tag = TAG, message = "Password change finished: $result")
    }

    companion object {
        private const val TAG = "ChangePasswordScreenStore"
    }
}

/**
 * Maps a password change result to the translation key of the message telling the user why the password wasn't changed, and the arguments it is formatted with.
 *
 * @receiver The result of the password change.
 * @return The translation key and its arguments.
 */
private fun PasswordResult<Unit>.toMessage(): Pair<String, ImmutableList<String>> = when (this) {
    is PasswordResult.Success, PasswordResult.Failed -> "change_password_failed" to persistentListOf()
    PasswordResult.SessionEnded -> "password_session_ended" to persistentListOf()
    is PasswordResult.Refused -> when (val failure = failure) {
        is PasswordFailure.TooShort -> "password_too_short" to persistentListOf(failure.minLength.toString())
        PasswordFailure.TooLong -> "password_too_long" to persistentListOf()
        is PasswordFailure.WrongPassword -> when (failure.remainingAttempts) {
            1 -> "password_wrong_last" to persistentListOf()
            else -> "password_wrong" to persistentListOf(failure.remainingAttempts.toString())
        }
    }
}
