package com.app.builder.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import com.app.builder.data.serializer.decode
import com.app.builder.data.serializer.encode
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class UserCredentialsTest: TestCase() {

    /** Verifies toString exposes the username but masks the password. */
    @Test
    fun toStringMasksPassword() = runUnitTest {
        val string = FakeData.credentials.toString()
        assertEquals(expected = "UserCredentials(username=user, password=********)", actual = string)
        assertFalse(actual = string.contains(other = FakeData.credentials.password))
    }

    /** Verifies credentials with the same username and password are equal and share a hash code. */
    @Test
    fun equalsAndHashCodeMatchOnSameValues() = runUnitTest {
        val copy = UserCredentials(username = FakeData.credentials.username, password = FakeData.credentials.password)
        assertEquals(expected = FakeData.credentials, actual = copy)
        assertEquals(expected = FakeData.credentials.hashCode(), actual = copy.hashCode())
    }

    /** Verifies credentials differing in username or password are not equal. */
    @Test
    fun equalsDiffersOnDifferentValues() = runUnitTest {
        val credentials = FakeData.credentials
        assertNotEquals(illegal = credentials, actual = UserCredentials(username = "other", password = credentials.password))
        assertNotEquals(illegal = credentials, actual = UserCredentials(username = credentials.username, password = "other"))
    }

    /** Verifies credentials survive a JSON round trip with the raw password intact. */
    @Test
    fun serializationRoundTrip() = runUnitTest {
        assertEquals(
            expected = FakeData.credentials,
            actual = decode(value = assertNotNull(actual = encode(value = FakeData.credentials)))
        )
    }
}
