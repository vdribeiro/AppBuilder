package com.app.builder.domain.usecase.buildinguser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.coroutines.flow.firstOrNull
import com.app.builder.core.config.ServerConfigs
import com.app.builder.core.config.ServerFlags
import com.app.builder.core.locale.UNIX_EPOCH
import com.app.builder.core.locale.now
import com.app.builder.core.security.uuid
import com.app.builder.data.database.reset
import com.app.builder.domain.usecase.Outcome
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class BuildingUserUseCasesTest: TestCase() {

    override suspend fun beforeTest() {
        super.beforeTest()
        ServerFlags.set { it.copy(push = true) }
    }

    /** Verifies that upserting a building user makes it retrievable and triggers a notification for the users that can read building users. */
    @Test
    fun crudBuildingUsers() = runServerTest {
        val buildingUserUseCases = dependency.get()
            .useCases
            .buildingUserUseCases
        val notificationsUseCases = dependency.get()
            .useCases
            .notificationsUseCases
        val lastSyncUtc = now() - 1.days

        assertTrue(actual = notificationsUseCases.observeNotifications(userUuid = FakeData.adminUser.uuid).firstOrNull().orEmpty().isEmpty())
        assertTrue(actual = buildingUserUseCases.getBuildingUsers(lastSyncUtc = lastSyncUtc, cursorUtc = null, cursorUuid = null, pageSize = ServerConfigs.value.pageSize).successData().isEmpty())
        assertEquals(expected = Outcome.Success(data = Unit), actual = buildingUserUseCases.upsertBuildingUser(buildingUser = FakeData.buildingUser))
        assertEquals(
            expected = listOf(element = FakeData.buildingUser),
            actual = buildingUserUseCases.getBuildingUsers(lastSyncUtc = lastSyncUtc, cursorUtc = null, cursorUuid = null, pageSize = ServerConfigs.value.pageSize).successData()
        )
        assertTrue(actual = notificationsUseCases.observeNotifications(userUuid = FakeData.adminUser.uuid).firstOrNull().orEmpty().isNotEmpty())
    }

    /** Verifies that deleting and relinking the same pair keeps a single building user, with the latest change. */
    @Test
    fun relinkingReusesTheLink() = runServerTest {
        val buildingUserUseCases = dependency.get()
            .useCases
            .buildingUserUseCases
        val link = FakeData.buildingUser

        buildingUserUseCases.upsertBuildingUser(buildingUser = link)
        buildingUserUseCases.upsertBuildingUser(buildingUser = link.copy(modifiedAt = link.modifiedAt + 1.seconds, deletedAt = link.modifiedAt + 1.seconds))
        val relinked = link.copy(modifiedAt = link.modifiedAt + 2.seconds)
        buildingUserUseCases.upsertBuildingUser(buildingUser = relinked)

        val links = buildingUserUseCases.getBuildingUsers(lastSyncUtc = Instant.UNIX_EPOCH, cursorUtc = null, cursorUuid = null, pageSize = ServerConfigs.value.pageSize).successData()
        assertEquals(expected = listOf(element = relinked), actual = links)
    }

    /** Verifies that fetching a single building user returns it, and not found for an unknown uuid. */
    @Test
    fun getBuildingUser() = runServerTest {
        val buildingUserUseCases = dependency.get()
            .useCases
            .buildingUserUseCases

        assertEquals(expected = Outcome.Success(data = Unit), actual = buildingUserUseCases.upsertBuildingUser(buildingUser = FakeData.buildingUser))
        assertEquals(expected = FakeData.buildingUser, actual = buildingUserUseCases.getBuildingUser(uuid = FakeData.buildingUser.uuid).successData())
        assertEquals(expected = Outcome.NotFound, actual = buildingUserUseCases.getBuildingUser(uuid = uuid()))
    }

    /** Verifies that when the database fails, its reads and writes report [Outcome.Error]. */
    @Test
    fun databaseFailureIsAnError() = runServerTest {
        val buildingUserUseCases = dependency.get()
            .useCases
            .buildingUserUseCases
        dependency.get().database.reset()

        listOf(
            "getBuildingUsers" to buildingUserUseCases.getBuildingUsers(lastSyncUtc = Instant.UNIX_EPOCH, cursorUtc = null, cursorUuid = null, pageSize = ServerConfigs.value.pageSize),
            "getBuildingUser" to buildingUserUseCases.getBuildingUser(uuid = FakeData.buildingUser.uuid),
            "upsertBuildingUser" to buildingUserUseCases.upsertBuildingUser(buildingUser = FakeData.buildingUser)
        ).forEach { (name, outcome) -> assertEquals(expected = Outcome.Error, actual = outcome, message = name) }
    }
}
