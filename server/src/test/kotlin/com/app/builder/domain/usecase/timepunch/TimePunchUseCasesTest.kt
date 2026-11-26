package com.app.builder.domain.usecase.timepunch

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import kotlinx.coroutines.flow.firstOrNull
import com.app.builder.core.config.ServerConfigs
import com.app.builder.core.config.ServerFlags
import com.app.builder.core.locale.UNIX_EPOCH
import com.app.builder.core.locale.now
import com.app.builder.core.security.uuid
import com.app.builder.data.database.reset
import com.app.builder.domain.EntityType
import com.app.builder.domain.Permission
import com.app.builder.domain.usecase.Outcome
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class TimePunchUseCasesTest: TestCase() {

    override suspend fun beforeTest() {
        super.beforeTest()
        ServerFlags.set { it.copy(pushSocket = true) }
    }

    /** Verifies that upserting a time punch makes it retrievable and triggers a notification for its user and for the users with a time punch permission, but for no other user. */
    @Test
    fun crudTimePunches() = runServerTest {
        val timePunchUseCases = dependency.get()
            .useCases
            .timePunchUseCases
        val notificationsUseCases = dependency.get()
            .useCases
            .notificationsUseCases
        val lastSyncUtc = now() - 1.days
        val other = seedUser(registrationForm = registrationForm(username = "other_time_punch_user", permissions = mapOf(EntityType.USER to Permission.READ)))

        assertTrue(actual = timePunchUseCases.getTimePunches(userUuid = null, lastSyncUtc = lastSyncUtc, cursorUtc = null, cursorUuid = null, pageSize = ServerConfigs.value.pageSize).successData().isEmpty())
        assertEquals(expected = Outcome.Success(data = Unit), actual = timePunchUseCases.upsertTimePunch(timePunch = FakeData.timePunch))
        assertEquals(expected = listOf(element = FakeData.timePunch), actual = timePunchUseCases.getTimePunches(userUuid = null, lastSyncUtc = lastSyncUtc, cursorUtc = null, cursorUuid = null, pageSize = ServerConfigs.value.pageSize).successData())

        // FakeData.user owns the time punch without any time punch permission, and the admin can write every entity type.
        assertTrue(actual = notificationsUseCases.observeNotifications(userUuid = FakeData.user.uuid).firstOrNull().orEmpty().isNotEmpty())
        assertTrue(actual = notificationsUseCases.observeNotifications(userUuid = FakeData.adminUser.uuid).firstOrNull().orEmpty().isNotEmpty())
        assertTrue(actual = notificationsUseCases.observeNotifications(userUuid = other.user.uuid).firstOrNull().orEmpty().isEmpty())
    }

    /** Verifies that listing the time punches of a user leaves out the time punches of every other user. */
    @Test
    fun getTimePunchesOfUser() = runServerTest {
        val timePunchUseCases = dependency.get()
            .useCases
            .timePunchUseCases
        val adminTimePunch = FakeData.timePunch.copy(uuid = uuid(), userUuid = FakeData.adminUser.uuid)

        assertEquals(expected = Outcome.Success(data = Unit), actual = timePunchUseCases.upsertTimePunch(timePunch = FakeData.timePunch))
        assertEquals(expected = Outcome.Success(data = Unit), actual = timePunchUseCases.upsertTimePunch(timePunch = adminTimePunch))

        assertEquals(
            expected = listOf(element = adminTimePunch),
            actual = timePunchUseCases.getTimePunches(userUuid = FakeData.adminUser.uuid, lastSyncUtc = Instant.UNIX_EPOCH, cursorUtc = null, cursorUuid = null, pageSize = ServerConfigs.value.pageSize).successData()
        )
        assertEquals(
            expected = 2,
            actual = timePunchUseCases.getTimePunches(userUuid = null, lastSyncUtc = Instant.UNIX_EPOCH, cursorUtc = null, cursorUuid = null, pageSize = ServerConfigs.value.pageSize).successData().size
        )
    }

    /** Verifies that fetching a single time punch returns it, and not found for an unknown uuid. */
    @Test
    fun getTimePunch() = runServerTest {
        val timePunchUseCases = dependency.get()
            .useCases
            .timePunchUseCases

        assertEquals(expected = Outcome.Success(data = Unit), actual = timePunchUseCases.upsertTimePunch(timePunch = FakeData.timePunch))
        assertEquals(expected = FakeData.timePunch, actual = timePunchUseCases.getTimePunch(uuid = FakeData.timePunch.uuid).successData())
        assertEquals(expected = Outcome.NotFound, actual = timePunchUseCases.getTimePunch(uuid = uuid()))
    }

    /** Verifies that when the database fails, its reads and writes report [Outcome.Error] rather than an empty list, a missing time punch or a refused write. */
    @Test
    fun databaseFailureIsAnError() = runServerTest {
        val timePunchUseCases = dependency.get()
            .useCases
            .timePunchUseCases
        dependency.get().database.reset()

        listOf(
            "getTimePunches" to timePunchUseCases.getTimePunches(userUuid = null, lastSyncUtc = Instant.UNIX_EPOCH, cursorUtc = null, cursorUuid = null, pageSize = ServerConfigs.value.pageSize),
            "getTimePunch" to timePunchUseCases.getTimePunch(uuid = FakeData.timePunch.uuid),
            "upsertTimePunch" to timePunchUseCases.upsertTimePunch(timePunch = FakeData.timePunch)
        ).forEach { (name, outcome) -> assertEquals(expected = Outcome.Error, actual = outcome, message = name) }
    }
}
