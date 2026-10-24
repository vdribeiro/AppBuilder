package com.app.builder.domain.storage

import kotlin.time.Duration

/**
 * An [ObjectStorageService] used when the bucket is not configured.
 * Signing returns null so callers refuse the upload rather than handing out a URL that cannot work, and an object is never reported as present.
 */
object NoOpObjectStorage: ObjectStorageService {
    override fun presignUpload(key: String, contentType: String, expiry: Duration): String? = null
    override suspend fun exists(key: String): Boolean = false
    override suspend fun delete(key: String): Boolean = true
}
