package com.app.builder.domain

import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable
import com.app.builder.core.locale.now
import com.app.builder.core.security.uuid

/**
 * File entity, without its content.
 *
 * @property uuid The unique, immutable identifier for this file.
 * @property modifiedAt The timestamp indicating when this file was last updated.
 * @property deletedAt The timestamp indicating when this file was last deleted.
 * @property name The name of the file.
 * @property contentType The media type of the content.
 * @property size The size of the content, in bytes.
 */
@Serializable
data class FileMetadata(
    val uuid: Uuid = uuid(),
    val modifiedAt: Instant = now(),
    val deletedAt: Instant? = null,
    val name: String = "",
    val contentType: String = "",
    val size: Long = 0L
)
