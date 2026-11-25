package com.app.builder.domain.usecase.authentication

import com.app.builder.domain.PasswordFailure

/** The outcome of registering an account. */
sealed interface RegisterOutcome {
    /** The account was created. */
    data object Success: RegisterOutcome

    /** The account belongs to another user than the one in the form. */
    data object Invalid: RegisterOutcome

    /**
     * The password is refused.
     *
     * @property failure Why it is refused.
     */
    data class InvalidPassword(val failure: PasswordFailure): RegisterOutcome

    /** The user already has an account or the username is taken. */
    data object Conflict: RegisterOutcome

    /** It failed on the server's side. */
    data object Error: RegisterOutcome
}
