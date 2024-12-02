package com.app.builder.data.storage

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import com.app.builder.test.TestCase

class StorageFileTest: TestCase() {

    override suspend fun beforeTest() {
        super.beforeTest()
        TestFile.all.forEach { it.reset() }
    }

    override suspend fun afterTest() {
        super.afterTest()
        TestFile.all.forEach { it.reset() }
    }

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
}
