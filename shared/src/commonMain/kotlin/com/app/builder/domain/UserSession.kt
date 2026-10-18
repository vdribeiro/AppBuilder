package com.app.builder.domain

import kotlinx.serialization.Serializable

/**
 * A user and a session verifier and tokens.
 *
 * @property user The [User] domain model.
 * @property username The username the session was opened with.
 * @property salt The random salt the verifier was derived with.
 * @property verifier The key derived from the password.
 * @property accessToken The JWT access token used for network authentication.
 * @property refreshToken The JWT refresh token used to renew the access token.
 */
@Serializable
data class UserSession(
    val user: User,
    val username: String,
    val salt: String,
    val verifier: String,
    val accessToken: String,
    val refreshToken: String
)