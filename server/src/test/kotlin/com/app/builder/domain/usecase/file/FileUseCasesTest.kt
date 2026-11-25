package com.app.builder.domain.usecase.file

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.toByteArray
import com.app.builder.core.security.uuid
import com.app.builder.domain.FileMetadata
import com.app.builder.domain.file.NoOpFileStorage
import com.app.builder.domain.usecase.Outcome
import com.app.builder.test.TestCase

class FileUseCasesTest: TestCase() {

    /** Verifies that uploading a file stores its content and its metadata with the size of the content. */
    @Test
    fun uploadFile() = runServerTest {
        val fileUseCases = dependency.get()
            .useCases
            .fileUseCases
        val content = "file content".encodeToByteArray()
        val file = FileMetadata(name = "notes.txt", contentType = "text/plain")

        val stored = fileUseCases.uploadFile(file = file, content = ByteReadChannel(content = content)).successData()

        assertEquals(expected = file.copy(size = content.size.toLong()), actual = stored)
        assertEquals(expected = Outcome.Success(data = stored), actual = fileUseCases.getFile(uuid = file.uuid))
        assertContentEquals(expected = content, actual = fileUseCases.readFile(uuid = file.uuid).successData().toByteArray())
    }

    /** Verifies that a file that was never uploaded is not found, neither its metadata nor its content. */
    @Test
    fun unknownFile() = runServerTest {
        val fileUseCases = dependency.get()
            .useCases
            .fileUseCases
        val uuid = uuid()

        assertEquals(expected = Outcome.NotFound, actual = fileUseCases.getFile(uuid = uuid))
        assertEquals(expected = Outcome.NotFound, actual = fileUseCases.readFile(uuid = uuid))
    }

    /** Verifies that an upload fails without storing metadata when the storage cannot store the content. */
    @Test
    fun uploadFileWithoutStorage() = runServerTest {
        val fileUseCases = FileGateway(database = dependency.get().database, fileStorage = NoOpFileStorage)
        val file = FileMetadata(name = "notes.txt", contentType = "text/plain")

        assertEquals(expected = Outcome.Error, actual = fileUseCases.uploadFile(file = file, content = ByteReadChannel(text = "file content")))
        assertEquals(expected = Outcome.NotFound, actual = fileUseCases.getFile(uuid = file.uuid))
    }
}
