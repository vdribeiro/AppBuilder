package com.app.builder.domain

import kotlinx.serialization.Serializable

/**
 * Access and refresh tokens used to authenticate remote server requests.
 *
 * @property accessToken The short-lived token supplied in HTTP headers to authorize immediate resource access.
 * @property refreshToken The long-lived token used to securely obtain an updated token pair without manual user re-authentication.
 */
@Serializable
data class BearerToken(
    val accessToken: String,
    val refreshToken: String
)