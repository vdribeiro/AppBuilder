package com.app.builder.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import com.app.builder.core.security.uuid
import com.app.builder.core.security.uuidOf
import com.app.builder.data.serializer.decode
import com.app.builder.data.serializer.encode
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class BuildingUserTest: TestCase() {

    /** Verifies a building user's uuid is derived from its building and user, whatever its timestamps, and differs for another pair. */
    @Test
    fun uuidIsDerivedFromThePair() = runUnitTest {
        val link = FakeData.buildingUser
        assertEquals(expected = uuidOf(link.buildingUuid, link.userUuid), actual = link.uuid)
        assertEquals(expected = link.uuid, actual = link.copy(deletedAt = link.modifiedAt).uuid)
        assertNotEquals(illegal = link.uuid, actual = link.copy(userUuid = uuid()).uuid)
    }

    /** Verifies a building user survives a JSON round trip without its uuid in the JSON, and derives the same uuid after decoding. */
    @Test
    fun serializationRoundTrip() = runUnitTest {
        val json = assertNotNull(actual = encode(value = FakeData.buildingUser))
        assertFalse(actual = "\"uuid\"" in json)

        val decoded = assertNotNull(actual = decode<BuildingUser>(value = json))
        assertEquals(expected = FakeData.buildingUser, actual = decoded)
        assertEquals(expected = FakeData.buildingUser.uuid, actual = decoded.uuid)
    }
}
