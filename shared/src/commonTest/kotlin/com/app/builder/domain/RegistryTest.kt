package com.app.builder.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import com.app.builder.core.platform.OS
import com.app.builder.data.serializer.decode
import com.app.builder.data.serializer.encode
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class RegistryTest: TestCase() {

    /** Verifies a registry survives a JSON round trip, including its OS and embedded payload. */
    @Test
    fun serializationRoundTrip() = runUnitTest {
        assertEquals(
            expected = FakeData.registry,
            actual = decode(value = assertNotNull(actual = encode(value = FakeData.registry)))
        )
    }

    /** Verifies a registry with no target entity or payload survives a JSON round trip for every OS. */
    @Test
    fun serializationRoundTripWithoutEntity() = runUnitTest {
        listOf(OS.Android, OS.Ios, OS.Windows, OS.Mac, OS.Linux, OS.Web, OS.Unknown).forEach { os ->
            val registry = FakeData.registry.copy(os = os, entityUuid = null, entityType = null, payload = null)
            assertEquals(
                expected = registry,
                actual = decode(value = assertNotNull(actual = encode(value = registry)))
            )
        }
    }
}
