package com.app.builder.domain.usecase.user

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import com.app.builder.core.config.ServerConfigs
import com.app.builder.core.locale.now
import com.app.builder.core.security.uuid
import com.app.builder.data.database.create
import com.app.builder.data.database.reset
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class UserUseCasesTest: TestCase() {

    /** Verifies that a user can be upserted, updated, and retrieved by UUID. */
    @Test
    fun upsertAndGetUser() = runServerTest {
        dependency.get().database.reset()
        dependency.get().database.create()

        val userUseCases = dependency.get()
            .useCases
            .userUseCases

        assertNull(actual = userUseCases.getUser(uuid = FakeData.user.uuid))

        assertTrue(actual = userUseCases.upsertUser(user = FakeData.user))
        assertEquals(expected = FakeData.user, actual = userUseCases.getUser(uuid = FakeData.user.uuid))

        val renamed = FakeData.user.copy(name = "Renamed")
        assertTrue(actual = userUseCases.upsertUser(user = renamed))
        assertEquals(expected = renamed, actual = userUseCases.getUser(uuid = FakeData.user.uuid))
    }

    /** Verifies that fetching users since a last sync timestamp only returns users modified after that time. */
    @Test
    fun getUsersSinceLastSync() = runServerTest {
        dependency.get().database.reset()
        dependency.get().database.create()

        val userUseCases = dependency.get()
            .useCases
            .userUseCases

        assertTrue(actual = userUseCases.getUsers(lastSyncUtc = Instant.fromEpochSeconds(epochSeconds = 0), cursorUtc = null, cursorUuid = null, pageSize = ServerConfigs.configs.pageSize).isEmpty())

        val now = now()
        val old = FakeData.user.copy(uuid = uuid(), modifiedAt = now - 10.minutes, name = "Old")
        val recent = FakeData.user.copy(uuid = uuid(), modifiedAt = now, name = "Recent")

        assertTrue(actual = userUseCases.upsertUser(user = old))
        assertTrue(actual = userUseCases.upsertUser(user = recent))

        assertEquals(expected = 2, actual = userUseCases.getUsers(lastSyncUtc = Instant.fromEpochSeconds(epochSeconds = 0), cursorUtc = null, cursorUuid = null, pageSize = ServerConfigs.configs.pageSize).size)
        assertEquals(expected = listOf(recent), actual = userUseCases.getUsers(lastSyncUtc = now - 1.minutes, cursorUtc = null, cursorUuid = null, pageSize = ServerConfigs.configs.pageSize))
        assertTrue(actual = userUseCases.getUsers(lastSyncUtc = now + 1.minutes, cursorUtc = null, cursorUuid = null, pageSize = ServerConfigs.configs.pageSize).isEmpty())
    }

    /** Verifies that fetching users paginates results using the cursor timestamp and UUID. */
    @Test
    fun getUsersPaginated() = runServerTest {
        dependency.get().database.reset()
        dependency.get().database.create()

        val userUseCases = dependency.get()
            .useCases
            .userUseCases

        val now = now()
        val old = FakeData.user.copy(uuid = uuid(), modifiedAt = now - 10.minutes, name = "Old")
        val recent = FakeData.user.copy(uuid = uuid(), modifiedAt = now, name = "Recent")

        assertTrue(actual = userUseCases.upsertUser(user = old))
        assertTrue(actual = userUseCases.upsertUser(user = recent))

        val firstPage = userUseCases.getUsers(lastSyncUtc = Instant.fromEpochSeconds(epochSeconds = 0), cursorUtc = null, cursorUuid = null, pageSize = 1)
        assertEquals(expected = listOf(recent), actual = firstPage)

        val secondPage = userUseCases.getUsers(
            lastSyncUtc = Instant.fromEpochSeconds(epochSeconds = 0),
            cursorUtc = firstPage.last().modifiedAt,
            cursorUuid = firstPage.last().uuid,
            pageSize = 1
        )
        assertEquals(expected = listOf(old), actual = secondPage)
    }
}
