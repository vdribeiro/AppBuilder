package com.app.builder.ui.screen.login

import kotlinx.coroutines.Job
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.domain.UserCredentials
import com.app.builder.domain.gateway.authentication.AuthenticationUseCases
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
        }
    }

    /**
     * Handles the login submission.
     *
     * @param state The current screen state, providing the username and password to submit.
     * @return The [Job] representing this execution.
     */
    private fun submitLogin(state: LoginScreenState): Job = launch(id = "submitLogin") {
        Telemetry.info(tag = TAG, message = "Attempting login for ${state.username}")

        updateState { it.copy(loading = true) }
        authenticationUseCases.login(credentials = UserCredentials(username = state.username, password = state.password))
        updateState { it.copy(loading = false) }

        Telemetry.info(tag = TAG, message = "Login finished")
    }

    companion object {
        private const val TAG = "LoginScreenStore"
    }
}
