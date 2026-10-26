package com.app.builder.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import com.app.builder.data.serializer.decode
import com.app.builder.data.serializer.encode
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class UserTest: TestCase() {

    /** Verifies a WRITE permission grants both WRITE and READ access to its entity type. */
    @Test
    fun writePermissionGrantsWriteAndRead() = runUnitTest {
        val user = FakeData.user.copy(permissions = mapOf(EntityType.TASK to Permission.WRITE))
        assertTrue(actual = user.hasPermission(entityType = EntityType.TASK, permission = Permission.WRITE))
        assertTrue(actual = user.hasPermission(entityType = EntityType.TASK, permission = Permission.READ))
    }

    /** Verifies a READ permission grants READ but not WRITE access to its entity type. */
    @Test
    fun readPermissionGrantsReadOnly() = runUnitTest {
        val user = FakeData.user.copy(permissions = mapOf(EntityType.TASK to Permission.READ))
        assertTrue(actual = user.hasPermission(entityType = EntityType.TASK, permission = Permission.READ))
        assertFalse(actual = user.hasPermission(entityType = EntityType.TASK, permission = Permission.WRITE))
    }

    /** Verifies an entity type missing from the permissions map grants neither READ nor WRITE access. */
    @Test
    fun missingPermissionGrantsNothing() = runUnitTest {
        val user = FakeData.user.copy(permissions = mapOf(EntityType.TASK to Permission.WRITE))
        assertFalse(actual = user.hasPermission(entityType = EntityType.USER, permission = Permission.READ))
        assertFalse(actual = user.hasPermission(entityType = EntityType.USER, permission = Permission.WRITE))
    }

    /** Verifies the admin fake user holds every permission while the regular fake user is limited to its granted entity types. */
    @Test
    fun fakeUsersPermissions() = runUnitTest {
        EntityType.entries.forEach { type ->
            assertTrue(actual = FakeData.adminUser.hasPermission(entityType = type, permission = Permission.WRITE))
        }
        assertTrue(actual = FakeData.user.hasPermission(entityType = EntityType.USER, permission = Permission.WRITE))
        assertTrue(actual = FakeData.user.hasPermission(entityType = EntityType.TASK, permission = Permission.READ))
        assertFalse(actual = FakeData.user.hasPermission(entityType = EntityType.TASK, permission = Permission.WRITE))
        assertFalse(actual = FakeData.user.hasPermission(entityType = EntityType.SERVER_FLAG, permission = Permission.READ))
    }

    /** Verifies a user survives a JSON round trip, including its permissions map. */
    @Test
    fun serializationRoundTrip() = runUnitTest {
        assertEquals(
            expected = FakeData.user,
            actual = decode(value = assertNotNull(actual = encode(value = FakeData.user)))
        )
    }
}
