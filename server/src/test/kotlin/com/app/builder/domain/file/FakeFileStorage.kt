package com.app.builder.domain.file

import kotlin.uuid.Uuid
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.toByteArray

/** A [FileStorage] keeping the content of files in memory. */
class FakeFileStorage: FileStorage {

    private val contents = mutableMapOf<Uuid, ByteArray>()

    override suspend fun write(uuid: Uuid, content: ByteReadChannel): Long? {
        val bytes = content.toByteArray()
        contents[uuid] = bytes
        return bytes.size.toLong()
    }

    override suspend fun read(uuid: Uuid): ByteReadChannel? = contents[uuid]?.let { ByteReadChannel(content = it) }
}
