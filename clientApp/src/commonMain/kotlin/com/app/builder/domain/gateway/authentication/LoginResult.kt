package com.app.builder.domain.gateway.authentication

import com.app.builder.domain.User

/** The result of a login. */
sealed interface LoginResult {
    /**
     * The user is logged in.
     *
     * @property user The logged in user.
     */
    data class Success(val user: User): LoginResult

    /** The server refused the username or the password. */
    data object WrongCredentials: LoginResult

    /** The credentials are right, but the account is disabled. */
    data object AccountDisabled: LoginResult

    /** The server refused the login for too many requests. */
    data object TooManyAttempts: LoginResult

    /** The server gave no answer and the credentials don't match a session stored on the device. */
    data object NoConnection: LoginResult

    /** The login failed for any other reason, such as a server error. */
    data object Failed: LoginResult
}
