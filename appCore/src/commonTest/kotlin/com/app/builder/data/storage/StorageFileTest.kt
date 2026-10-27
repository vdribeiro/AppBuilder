package com.app.builder.data.storage

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.nullable
import com.app.builder.core.security.decrypt
import com.app.builder.data.serializer.encode
import com.app.builder.test.TestCase

class StorageFileTest: TestCase() {

    /** Verifies that [CoreFile] entries expose their expected paths. */
    @Test
    fun coreFilePaths() = runUnitTest {
        assertEquals(expected = "device", actual = CoreFile.Device.path)
    }

    /** Verifies that a value saved through [StorageFile.save] can be read back unchanged through [StorageFile.load]. */
    @Test
    fun saveThenLoadRoundTripsTheValue() = runUnitTest {
        val document = TestFile.Document(name = "doc", count = 3)

        assertTrue(actual = TestFile.Secret.save { document })

        assertEquals(expected = document, actual = TestFile.Secret.load())
    }

    /** Verifies that reading through [StorageFile.cache] after a save serves the saved value from the in-memory cache. */
    @Test
    fun cacheServesTheSavedValue() = runUnitTest {
        val document = TestFile.Document(name = "cached", count = 1)
        TestFile.Secret.save { document }

        val cache = TestFile.Secret.cache()

        assertEquals(expected = document, actual = cache.value)
    }

    /** Verifies that saving a null value deletes the underlying file, so a subsequent load returns null. */
    @Test
    fun savingNullValueDeletesTheFile() = runUnitTest {
        TestFile.Secret.save { TestFile.Document(name = "temp", count = 1) }

        assertTrue(actual = TestFile.Secret.save { null })

        assertNull(actual = TestFile.Secret.load())
    }

    /** Verifies that a key-value backed [StorageFile] round trips through save and load as well. */
    @Test
    fun keyValueFileRoundTripsThroughSaveAndLoad() = runUnitTest {
        assertTrue(actual = TestFile.Keys.save { mapOf(TestFile.Keys.Key.ALPHA to "1") })

        val loaded = TestFile.Keys.load()

        assertEquals(expected = "1", actual = loaded?.get(TestFile.Keys.Key.ALPHA))
    }

    /** Verifies that [StorageFile.save] merges into the previously cached value rather than replacing it wholesale. */
    @Test
    fun saveMergesOntoThePreviousCachedValue() = runUnitTest {
        TestFile.Keys.save { mapOf(TestFile.Keys.Key.ALPHA to "1") }

        TestFile.Keys.save { previous -> previous.orEmpty().plus(pair = (TestFile.Keys.Key.BETA to "2")) }

        val loaded = TestFile.Keys.load()
        assertEquals(expected = "1", actual = loaded?.get(TestFile.Keys.Key.ALPHA))
        assertEquals(expected = "2", actual = loaded?.get(TestFile.Keys.Key.BETA))
    }

    /** Verifies that [StorageFile.reset] deletes the file and clears the cache. */
    @Test
    fun resetDeletesTheFileAndClearsTheCache() = runUnitTest {
        TestFile.Secret.save { TestFile.Document(name = "temp", count = 1) }

        assertTrue(actual = TestFile.Secret.reset())

        assertNull(actual = TestFile.Secret.cache().value)
        assertNull(actual = TestFile.Secret.load())
    }

    /** Verifies that an unencrypted file is written as plain JSON, while an encrypted file is written through the cipher and decrypts back to the same JSON. */
    @Test
    fun encryptionFlagControlsTheStoredContent() = runUnitTest {
        val document = TestFile.Document(name = "doc", count = 2)
        val plainJson = encode(serializer = TestFile.Document.serializer(), value = document)

        TestFile.Plain.save { document }
        TestFile.Secret.save { document }

        assertEquals(expected = plainJson, actual = loadFile(path = TestFile.Plain.path))
        assertEquals(expected = plainJson, actual = loadFile(path = TestFile.Secret.path)?.let { decrypt(content = it) })
    }

    /** Verifies that the first [StorageFile.cache] read of a file that already exists on disk hydrates the cache from it. */
    @Test
    fun cacheHydratesFromAFileAlreadyOnDisk() = runUnitTest {
        val document = TestFile.Document(name = "disk", count = 4)
        val file = object: StorageFile<TestFile.Document?>(path = "test-hydrate", encrypted = false) {
            override val serializer: KSerializer<TestFile.Document?> = TestFile.Document.serializer().nullable
        }
        saveFile(path = file.path, content = assertNotNull(actual = encode(serializer = TestFile.Document.serializer(), value = document)))

        try {
            assertEquals(expected = document, actual = file.cache().value)
        } finally {
            deleteFile(path = file.path)
        }
    }

    /** Verifies that a file without a cache layer hands the value currently on disk to [StorageFile.save] and reads it back through [StorageFile.load]. */
    @Test
    fun uncachedFileReadsThePreviousValueFromDisk() = runUnitTest {
        TestFile.Plain.save { TestFile.Document(name = "doc", count = 1) }

        TestFile.Plain.save { previous -> previous?.copy(count = previous.count + 1) }

        assertEquals(expected = TestFile.Document(name = "doc", count = 2), actual = TestFile.Plain.load())
    }

    /** Verifies that a file without a cache layer never hydrates its cache flow, so [StorageFile.cache] holds null even with a value on disk. */
    @Test
    fun uncachedFileDoesNotHydrateItsCacheFlow() = runUnitTest {
        TestFile.Plain.save { TestFile.Document(name = "doc", count = 1) }

        assertNull(actual = TestFile.Plain.cache().value)
    }

    /** Verifies that a value that cannot be encoded is not saved and the cache rolls back to the previous value. */
    @Test
    fun unencodableValueIsRejectedAndRollsBackTheCache() = runUnitTest {
        assertTrue(actual = TestFile.Number.save { 1.0 })

        assertFalse(actual = TestFile.Number.save { Double.NaN })

        assertEquals(expected = 1.0, actual = TestFile.Number.cache().value)
        assertEquals(expected = 1.0, actual = TestFile.Number.load())
    }

    /** Verifies that [CoreFile.Device] round trips every one of its keys through disk. */
    @Test
    fun coreDeviceFileRoundTripsItsKeys() = runUnitTest {
        val values = mapOf(CoreFile.Device.Key.UUID to "uuid", CoreFile.Device.Key.CLOCK_OFFSET to "1s")

        assertTrue(actual = CoreFile.Device.save { values })

        assertEquals(expected = values, actual = CoreFile.Device.load())
    }
}
