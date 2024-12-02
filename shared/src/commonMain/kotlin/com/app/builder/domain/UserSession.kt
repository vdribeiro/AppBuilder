package com.app.builder.domain

import kotlinx.serialization.Serializable

/**
 * A user and a session hash and tokens.
 *
 * @property user The [User] domain model.
 * @property hash The generated session hash used for validating local credentials.
 * @property accessToken The JWT access token used for network authentication.
 * @property refreshToken The JWT refresh token used to renew the access token.
 */
@Serializable
data class UserSession(
    val user: User,
    val hash: String,
    val accessToken: String,
    val refreshToken: String
)