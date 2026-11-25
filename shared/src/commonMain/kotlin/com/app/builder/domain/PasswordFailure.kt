package com.app.builder.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Why the server refused a password. */
@Serializable
sealed interface PasswordFailure {

    /**
     * The new password is blank or shorter than the minimum length.
     *
     * @property minLength The minimum number of characters.
     */
    @Serializable
    @SerialName(value = "too_short")
    data class TooShort(val minLength: Int): PasswordFailure

    /** The new password is longer than the maximum number of bytes. */
    @Serializable
    @SerialName(value = "too_long")
    data object TooLong: PasswordFailure

    /**
     * The current password is wrong.
     *
     * @property remainingAttempts The number of wrong current passwords the session may still give before it is ended.
     */
    @Serializable
    @SerialName(value = "wrong_password")
    data class WrongPassword(val remainingAttempts: Int): PasswordFailure
}
