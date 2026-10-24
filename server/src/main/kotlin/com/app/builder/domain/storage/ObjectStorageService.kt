package com.app.builder.domain.storage

import kotlin.time.Duration

/** Grants time limited access to objects in a bucket, and inspects what is stored there. */
interface ObjectStorageService {

    /**
     * Builds a URL the holder can upload a single object to, without credentials of their own.
     *
     * @param key The object key to upload to.
     * @param contentType The media type the upload must declare.
     * @param expiry How long the returned URL stays valid.
     * @return The upload URL, or null if one could not be signed.
     */
    fun presignUpload(key: String, contentType: String, expiry: Duration): String?

    /**
     * Reports whether an object is present.
     *
     * @param key The object key to look for.
     * @return `true` if the object exists.
     */
    suspend fun exists(key: String): Boolean

    /**
     * Removes an object, succeeding whether or not it was there.
     *
     * @param key The object key to remove.
     * @return `true` if the object is no longer stored.
     */
    suspend fun delete(key: String): Boolean
}
