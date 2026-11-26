package com.app.builder.domain.usecase.authentication

import com.app.builder.domain.Authentication

/** The outcome of a login. */
sealed interface LoginOutcome {
    /**
     * The user is logged in.
     *
     * @property authentication The user, its account and the tokens of the new session.
     */
    data class Success(val authentication: Authentication): LoginOutcome

    /** The user is unknown or the password is wrong. */
    data object Rejected: LoginOutcome

    /** The password is right, but the account is disabled. */
    data object Disabled: LoginOutcome

    /** The username is locked after too many failed logins. */
    data object Locked: LoginOutcome

    /** It failed on the server's side. */
    data object Error: LoginOutcome
}
