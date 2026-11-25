package com.app.builder.domain.file

import kotlin.uuid.Uuid
import io.ktor.utils.io.ByteReadChannel

/** A no-op [FileStorage] that stores nothing. */
object NoOpFileStorage: FileStorage {
    override suspend fun write(uuid: Uuid, content: ByteReadChannel): Long? = null
    override suspend fun read(uuid: Uuid): ByteReadChannel? = null
}
