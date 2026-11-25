package com.app.builder.domain.file

import kotlin.test.Test
import kotlin.test.assertNull
import io.ktor.utils.io.ByteReadChannel
import com.app.builder.core.security.uuid
import com.app.builder.test.TestCase

class NoOpFileStorageTest: TestCase() {

    /** Verifies that writing stores nothing and reading finds nothing. */
    @Test
    fun storesNothing() = runServerTest {
        val uuid = uuid()
        assertNull(actual = NoOpFileStorage.write(uuid = uuid, content = ByteReadChannel(text = "content")))
        assertNull(actual = NoOpFileStorage.read(uuid = uuid))
    }
}
