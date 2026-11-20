package com.app.builder.domain.gateway.registry

import kotlin.test.Test
import kotlin.test.assertEquals
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class MappersTest: TestCase() {

    /** Verifies that a registry survives a round trip through its database schema, operating system included, and keeps the joined user name. */
    @Test
    fun registryRoundTrip() = runUnitTest {
        val schema = FakeData.registry.toRegistrySchema()
        val entry = toRegistryEntry(
            requestUuid = schema.requestUuid,
            requestType = schema.requestType,
            requestUtc = schema.requestUtc,
            requestSentUtc = schema.requestSentUtc,
            userUuid = schema.userUuid,
            appVersion = schema.appVersion,
            os = schema.os,
            osVersion = schema.osVersion,
            brand = schema.brand,
            model = schema.model,
            deviceUuid = schema.deviceUuid,
            entityUuid = schema.entityUuid,
            entityType = schema.entityType,
            payload = schema.payload,
            userName = FakeData.adminUser.name
        )

        assertEquals(expected = FakeData.registry.os.toString(), actual = schema.os)
        assertEquals(expected = RegistryEntry(registry = FakeData.registry, userName = FakeData.adminUser.name), actual = entry)
    }
}
