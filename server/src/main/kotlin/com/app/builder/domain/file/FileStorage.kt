package com.app.builder.domain.file

import kotlin.uuid.Uuid
import io.ktor.utils.io.ByteReadChannel

/** Stores the content of files. */
interface FileStorage {

    /**
     * Writes the content of the file with [uuid], replacing any previous content.
     *
     * @param uuid The file's unique identifier.
     * @param content The content to write.
     * @return The number of bytes written, or null when the content could not be stored.
     */
    suspend fun write(uuid: Uuid, content: ByteReadChannel): Long?

    /**
     * Reads the content of the file with [uuid].
     *
     * @param uuid The file's unique identifier.
     * @return The content, or null when there is none.
     */
    suspend fun read(uuid: Uuid): ByteReadChannel?
}
