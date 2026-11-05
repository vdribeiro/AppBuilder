package com.app.builder.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import com.app.builder.data.serializer.decode
import com.app.builder.data.serializer.encode
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class UserTest: TestCase() {

    /** Verifies a user survives a JSON round trip. */
    @Test
    fun serializationRoundTrip() = runUnitTest {
        assertEquals(
            expected = FakeData.user,
            actual = decode(value = assertNotNull(actual = encode(value = FakeData.user)))
        )
    }
}
