package com.app.builder.domain

import kotlinx.serialization.Serializable

/**
 * Credentials used for user authentication.
 *
 * @property username The unique username identity for the account.
 * @property password The account password.
 */
@Serializable
class UserCredentials(
    val username: String,
    val password: String
) {
    /**
     * Returns a string representation of the credentials with the password masked.
     *
     * @return A sanitized string excluding the raw password value.
     */
    override fun toString(): String =
        "UserCredentials(username=$username, password=********)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as UserCredentials
        if (username != other.username) return false
        if (password != other.password) return false
        return true
    }

    override fun hashCode(): Int {
        var result = username.hashCode()
        result = 31 * result + password.hashCode()
        return result
    }
}