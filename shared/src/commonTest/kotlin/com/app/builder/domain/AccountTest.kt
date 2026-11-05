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

class AccountTest: TestCase() {

    /** Verifies a WRITE permission grants both WRITE and READ access to its entity type. */
    @Test
    fun writePermissionGrantsWriteAndRead() = runUnitTest {
        val account = FakeData.account.copy(permissions = mapOf(EntityType.TASK to Permission.WRITE))
        assertTrue(actual = account.hasPermission(entityType = EntityType.TASK, permission = Permission.WRITE))
        assertTrue(actual = account.hasPermission(entityType = EntityType.TASK, permission = Permission.READ))
    }

    /** Verifies a READ permission grants READ but not WRITE access to its entity type. */
    @Test
    fun readPermissionGrantsReadOnly() = runUnitTest {
        val account = FakeData.account.copy(permissions = mapOf(EntityType.TASK to Permission.READ))
        assertTrue(actual = account.hasPermission(entityType = EntityType.TASK, permission = Permission.READ))
        assertFalse(actual = account.hasPermission(entityType = EntityType.TASK, permission = Permission.WRITE))
    }

    /** Verifies an entity type missing from the permissions map grants neither READ nor WRITE access. */
    @Test
    fun missingPermissionGrantsNothing() = runUnitTest {
        val account = FakeData.account.copy(permissions = mapOf(EntityType.TASK to Permission.WRITE))
        assertFalse(actual = account.hasPermission(entityType = EntityType.USER, permission = Permission.READ))
        assertFalse(actual = account.hasPermission(entityType = EntityType.USER, permission = Permission.WRITE))
    }

    /** Verifies the admin fake account holds every permission while the regular fake account is limited to its granted entity types. */
    @Test
    fun fakeAccountsPermissions() = runUnitTest {
        EntityType.entries.forEach { type ->
            assertTrue(actual = FakeData.adminAccount.hasPermission(entityType = type, permission = Permission.WRITE))
        }
        assertTrue(actual = FakeData.account.hasPermission(entityType = EntityType.USER, permission = Permission.READ))
        assertFalse(actual = FakeData.account.hasPermission(entityType = EntityType.USER, permission = Permission.WRITE))
        assertTrue(actual = FakeData.account.hasPermission(entityType = EntityType.TASK, permission = Permission.READ))
        assertFalse(actual = FakeData.account.hasPermission(entityType = EntityType.TASK, permission = Permission.WRITE))
        assertFalse(actual = FakeData.account.hasPermission(entityType = EntityType.SERVER_FLAG, permission = Permission.READ))
    }

    /** Verifies an account survives a JSON round trip, including its permissions map. */
    @Test
    fun serializationRoundTrip() = runUnitTest {
        assertEquals(
            expected = FakeData.account,
            actual = decode(value = assertNotNull(actual = encode(value = FakeData.account)))
        )
    }
}
