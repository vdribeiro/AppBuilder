package com.app.builder.domain.usecase.connection

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.upsert
import com.app.builder.core.config.ServerConfigs
import com.app.builder.core.locale.now
import com.app.builder.core.security.uuid
import com.app.builder.data.database.safeTransaction
import com.app.builder.data.database.table.ConnectionTable
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class ConnectionUseCasesTest: TestCase() {

    /** Verifies that connections can be added, queried, and removed correctly. */
    @Test
    fun addGetRemoveConnections() = runServerTest {
        val connectionUseCases = dependency.get()
            .useCases
            .connectionUseCases

        val instanceId = uuid()
        val deviceUuid = uuid()

        assertTrue(actual = connectionUseCases.getConnectedDeviceUuids(userUuid = FakeData.user.uuid).isEmpty())
        assertFalse(actual = connectionUseCases.removeConnection(deviceUuid = deviceUuid, instanceId = instanceId))

        assertTrue(actual = connectionUseCases.addConnection(userUuid = FakeData.user.uuid, deviceUuid = deviceUuid, instanceId = instanceId))
        assertEquals(expected = listOf(deviceUuid), actual = connectionUseCases.getConnectedDeviceUuids(userUuid = FakeData.user.uuid))

        assertTrue(actual = connectionUseCases.addConnection(userUuid = FakeData.user.uuid, deviceUuid = deviceUuid, instanceId = instanceId))
        assertEquals(expected = listOf(deviceUuid), actual = connectionUseCases.getConnectedDeviceUuids(userUuid = FakeData.user.uuid))

        assertFalse(actual = connectionUseCases.removeConnection(deviceUuid = deviceUuid, instanceId = uuid()))
        assertTrue(actual = connectionUseCases.removeConnection(deviceUuid = deviceUuid, instanceId = instanceId))
        assertTrue(actual = connectionUseCases.getConnectedDeviceUuids(userUuid = FakeData.user.uuid).isEmpty())
    }

    /** Verifies that the per-user connection limit rejects additional connections beyond the configured maximum. */
    @Test
    fun perUserLimitIsEnforced() = runServerTest {
        ServerConfigs.set { it.copy(pushMaxUserConnections = 1) }
        val connectionUseCases = dependency.get()
            .useCases
            .connectionUseCases

        val instanceId = uuid()

        assertTrue(actual = connectionUseCases.addConnection(userUuid = FakeData.user.uuid, deviceUuid = uuid(), instanceId = instanceId))
        assertFalse(actual = connectionUseCases.addConnection(userUuid = FakeData.user.uuid, deviceUuid = uuid(), instanceId = instanceId))
    }

    /** Verifies that the global connection limit rejects connections from other users once reached. */
    @Test
    fun globalLimitIsEnforced() = runServerTest {
        val otherUser = FakeData.user.copy(uuid = uuid(), permissions = emptyMap(), name = "Other")
        assertTrue(actual = dependency.get().useCases.userUseCases.upsertUser(user = otherUser))

        ServerConfigs.set { it.copy(pushMaxConnections = 1) }
        val connectionUseCases = dependency.get()
            .useCases
            .connectionUseCases

        val instanceId = uuid()

        assertTrue(actual = connectionUseCases.addConnection(userUuid = FakeData.user.uuid, deviceUuid = uuid(), instanceId = instanceId))
        assertFalse(actual = connectionUseCases.addConnection(userUuid = otherUser.uuid, deviceUuid = uuid(), instanceId = instanceId))
    }

    /** Verifies that touching connections keeps owned ones alive while removing stale connections from other instances. */
    @Test
    fun touchKeepsOwnedConnectionsAliveAndReapsStaleOnes() = runServerTest {
        val database = dependency.get().database
        val connectionUseCases = dependency.get()
            .useCases
            .connectionUseCases

        val instanceId = uuid()
        val deviceUuid = uuid()
        val staleDeviceUuid = uuid()

        assertTrue(actual = connectionUseCases.addConnection(userUuid = FakeData.user.uuid, deviceUuid = deviceUuid, instanceId = instanceId))

        database.safeTransaction {
            ConnectionTable.upsert {
                it[ConnectionTable.deviceUuid] = staleDeviceUuid
                it[ConnectionTable.userUuid] = FakeData.user.uuid
                it[ConnectionTable.instanceId] = uuid()
                it[ConnectionTable.updatedAt] = now() - 1.hours
            }
        }.getOrThrow()
        assertEquals(expected = setOf(deviceUuid, staleDeviceUuid), actual = database.storedDeviceUuids())

        connectionUseCases.touchConnections(instanceId = instanceId)
        assertEquals(expected = setOf(deviceUuid), actual = database.storedDeviceUuids())
        assertEquals(expected = listOf(deviceUuid), actual = connectionUseCases.getConnectedDeviceUuids(userUuid = FakeData.user.uuid))
    }

    /**
     * Reads the connection table directly, bypassing the use cases under test.
     *
     * @return The device UUIDs currently stored in the connection table.
     */
    private suspend fun R2dbcDatabase.storedDeviceUuids(): Set<Uuid> = safeTransaction {
        ConnectionTable.selectAll().map { it[ConnectionTable.deviceUuid] }.toList().toSet()
    }.getOrThrow()

    /** Verifies that connections older than the presence TTL are not reported as connected. */
    @Test
    fun staleConnectionsAreNotReported() = runServerTest {
        val connectionUseCases = dependency.get()
            .useCases
            .connectionUseCases

        assertTrue(actual = connectionUseCases.addConnection(userUuid = FakeData.user.uuid, deviceUuid = uuid(), instanceId = uuid()))

        ServerConfigs.set { it.copy(presenceTtl = -1_000L) }
        assertTrue(actual = connectionUseCases.getConnectedDeviceUuids(userUuid = FakeData.user.uuid).isEmpty())
    }
}
