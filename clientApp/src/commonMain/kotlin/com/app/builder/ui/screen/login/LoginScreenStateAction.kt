package com.app.builder.ui.screen.login

/** Actions supported by the Login Screen. */
sealed interface LoginScreenAction {
    /**
     * Updates the username input field.
     *
     * @param username The new username value.
     */
    data class UpdateUsername(val username: String): LoginScreenAction
    /**
     * Updates the password input field.
     *
     * @param password The new password value.
     */
    data class UpdatePassword(val password: String): LoginScreenAction
    /** Triggers the login submission process. */
    data object Submit: LoginScreenAction
}

/**
 * State of the Login Screen.
 *
 * @property loading Whether a login request is currently in flight.
 * @property username Username input field value.
 * @property password Password input field value.
 */
data class LoginScreenState(
    val loading: Boolean = false,
    val username: String = "",
    val password: String = "",
)
