package com.app.builder.domain

import kotlinx.serialization.Serializable

/**
 * A granted upload of a single avatar image.
 * The client sends the bytes straight to [url], so they never pass through the server, then stores [key] on its user.
 *
 * @property key The object key the image is stored under, and the value [User.avatar] holds.
 * @property url The address to PUT the image to. Valid briefly, for one object, and for one media type.
 * @property contentType The media type the upload must declare, which the signature covers.
 */
@Serializable
data class AvatarUpload(
    val key: String,
    val url: String,
    val contentType: String
)
