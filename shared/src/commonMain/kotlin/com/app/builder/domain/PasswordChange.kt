package com.app.builder.domain

import kotlinx.serialization.Serializable

/**
 * Password change of the account a session belongs to.
 *
 * @property refreshToken The refresh token of the session changing the password.
 * @property currentPassword The current login password of the account.
 * @property newPassword The login password to change to.
 */
@Serializable
data class PasswordChange(
    val refreshToken: String,
    val currentPassword: String,
    val newPassword: String
) {
    /**
     * Returns a string representation of the password change with the refresh token and passwords masked.
     *
     * @return A sanitized string excluding the raw refresh token and password values.
     */
    override fun toString(): String =
        "PasswordChange(refreshToken=********, currentPassword=********, newPassword=********)"
}
