package com.app.builder.domain.usecase.authentication

import kotlin.uuid.Uuid
import com.app.builder.domain.PasswordFailure

/** The outcome of changing a password. */
sealed interface PasswordChangeOutcome {
    /**
     * The password was changed.
     *
     * @property userUuid The uuid of the user the account belongs to.
     */
    data class Success(val userUuid: Uuid): PasswordChangeOutcome

    /** No session matches, the session is expired, the account is disabled or the session was ended by its last wrong current password. */
    data object Rejected: PasswordChangeOutcome

    /**
     * The current password is wrong or the new one is refused.
     *
     * @property failure Why it is refused.
     */
    data class InvalidPassword(val failure: PasswordFailure): PasswordChangeOutcome

    /** It failed on the server's side. */
    data object Error: PasswordChangeOutcome
}
