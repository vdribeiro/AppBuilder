package com.app.builder.data.storage

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import com.app.builder.core.platform.appCachePath
import com.app.builder.core.platform.appDataPath
import com.app.builder.data.serializer.encode
import com.app.builder.test.TestCase

class FileTest: TestCase() {

    /** An ASCII document, so the character count of its unencrypted encoding matches its size in bytes. */
    private val document = TestFile.Document(name = "doc", count = 1)

    /** Verifies that [listFiles] reports a saved file under the data directory with its name and size. */
    @Test
    fun listFilesReportsASavedFile() = runUnitTest {
        assertTrue(actual = TestFile.Plain.save { document })

        val file = assertNotNull(actual = listFiles(path = appDataPath).firstOrNull { it.name == TestFile.Plain.path })

        val content = assertNotNull(actual = encode(serializer = TestFile.Plain.serializer, value = document))
        assertEquals(expected = content.length.toLong(), actual = file.size)
    }

    /** Verifies that [listFiles] no longer reports a file once it has been deleted. */
    @Test
    fun listFilesOmitsADeletedFile() = runUnitTest {
        TestFile.Plain.save { document }

        assertTrue(actual = TestFile.Plain.reset())

        assertTrue(actual = listFiles(path = appDataPath).none { it.name == TestFile.Plain.path })
    }

    /** Verifies that [clearCache] succeeds and leaves the cache directory without files. */
    @Test
    fun clearCacheEmptiesTheCacheDirectory() = runUnitTest {
        assertTrue(actual = clearCache())

        assertTrue(actual = listFiles(path = appCachePath).isEmpty())
    }

    /** Verifies that [openFile] reports failure for a file that does not exist rather than launching a handler. */
    @Test
    fun openFileFailsForAMissingFile() = runUnitTest {
        assertFalse(actual = openFile(path = "$appDataPath/${TestFile.Plain.path}"))
    }
}
