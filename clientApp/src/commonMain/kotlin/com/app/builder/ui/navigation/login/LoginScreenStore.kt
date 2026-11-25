package com.app.builder.ui.navigation.login

import kotlinx.coroutines.Job
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.domain.UserCredentials
import com.app.builder.domain.gateway.authentication.AuthenticationUseCases
import com.app.builder.domain.gateway.authentication.LoginResult
import com.app.builder.ui.store.Store

/**
 * Store backing the Login Screen, handling credential updates and login submission.
 *
 * @param state Initial Login Screen state.
 * @property authenticationUseCases Use cases used to submit the login attempt.
 */
class LoginScreenStore(
    state: LoginScreenState,
    private val authenticationUseCases: AuthenticationUseCases
): Store<LoginScreenState, LoginScreenAction>(initialState = state) {
    override fun reducer(state: LoginScreenState, action: LoginScreenAction) {
        super.reducer(state = state, action = action)
        when (action) {
            is LoginScreenAction.UpdateUsername -> updateState { it.copy(username = action.username) }
            is LoginScreenAction.UpdatePassword -> updateState { it.copy(password = action.password) }
            LoginScreenAction.Submit -> submitLogin(state = state)
            LoginScreenAction.DismissMessage -> updateState { it.copy(message = null) }
        }
    }

    /**
     * Handles the login submission, showing a message telling why when the login fails.
     *
     * @param state The current screen state, providing the username and password to submit.
     * @return The [Job] representing this execution.
     */
    private fun submitLogin(state: LoginScreenState): Job = launch(id = "submitLogin") {
        Telemetry.info(tag = TAG, message = "Attempting login for ${state.username}")

        updateState { it.copy(loading = true, message = null) }
        val result = authenticationUseCases.login(credentials = UserCredentials(username = state.username, password = state.password))
        updateState { it.copy(loading = false, message = result.toMessage()) }

        Telemetry.info(tag = TAG, message = "Login finished")
    }

    companion object {
        private const val TAG = "LoginScreenStore"
    }
}

/**
 * Maps a login result to the translation key of the message telling the user why the login failed.
 *
 * @receiver The result of the login.
 * @return The translation key, or `null` for [LoginResult.Success].
 */
private fun LoginResult.toMessage(): String? = when (this) {
    is LoginResult.Success -> null
    LoginResult.WrongCredentials -> "login_wrong_credentials"
    LoginResult.AccountDisabled -> "login_account_disabled"
    LoginResult.TooManyAttempts -> "login_too_many_attempts"
    LoginResult.NoConnection -> "login_no_connection"
    LoginResult.Failed -> "login_failed"
}
