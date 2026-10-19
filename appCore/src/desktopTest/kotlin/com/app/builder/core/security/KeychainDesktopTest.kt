package com.app.builder.core.security

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import com.app.builder.test.TestCase

/** Desktop-specific coverage for the secret backing the keystore password, which only exists on this target. */
class KeychainDesktopTest: TestCase() {

    /** Verifies that an existing secret is returned as it stands, without replacing it. */
    @Test
    fun getOrCreateSecretKeepsAStoredSecret() = runUnitTest {
        val store = FakeSecretStore(secret = "stored")

        assertEquals(expected = "stored", actual = getOrCreateSecret(store = store))
        assertEquals(expected = 0, actual = store.writes)
    }

    /** Verifies that an empty store is filled with a generated secret, and that the same secret is returned afterwards. */
    @Test
    fun getOrCreateSecretGeneratesAndStoresASecret() = runUnitTest {
        val store = FakeSecretStore()
        val generated = assertNotNull(actual = getOrCreateSecret(store = store))

        assertEquals(expected = 1, actual = store.writes)
        assertEquals(expected = generated, actual = store.secret)
        assertEquals(expected = generated, actual = getOrCreateSecret(store = store))
        assertEquals(expected = 1, actual = store.writes)
    }

    /** Verifies that generated secrets differ between stores, so one machine's secret says nothing about another's. */
    @Test
    fun getOrCreateSecretGeneratesADistinctSecretEachTime() = runUnitTest {
        assertNotEquals(illegal = getOrCreateSecret(store = FakeSecretStore()), actual = getOrCreateSecret(store = FakeSecretStore()))
    }

    /** Verifies that a store refusing to hold the secret resolves to null, rather than to a value that would be lost on the next run. */
    @Test
    fun getOrCreateSecretRejectsAStoreThatCannotHoldTheSecret() = runUnitTest {
        assertNull(actual = getOrCreateSecret(store = FakeSecretStore(writable = false)))
        assertNull(actual = getOrCreateSecret(store = FakeSecretStore(persists = false)))
    }

    /** Verifies that a store reporting a blank secret is treated as empty and filled. */
    @Test
    fun getOrCreateSecretReplacesABlankSecret() = runUnitTest {
        val store = FakeSecretStore(secret = "")

        assertNotNull(actual = getOrCreateSecret(store = store))
        assertEquals(expected = 1, actual = store.writes)
    }

    /**
     * A [SecretStore] held in memory.
     *
     * @property secret The secret currently held.
     * @property writable Whether a write is accepted.
     * @property persists Whether an accepted write is actually kept.
     */
    private class FakeSecretStore(
        var secret: String? = null,
        private val writable: Boolean = true,
        private val persists: Boolean = true
    ): SecretStore {
        /** How many writes were attempted. */
        var writes: Int = 0
            private set

        override fun read(): String? = secret

        override fun write(secret: String): Boolean {
            writes++
            if (!writable) return false
            if (persists) this.secret = secret
            return true
        }
    }
}
