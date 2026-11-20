package com.app.builder.domain.usecase.equipmentuser

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

class EquipmentUserUseCasesTest: TestCase() {

    override suspend fun beforeTest() {
        super.beforeTest()
        ServerFlags.set { it.copy(push = true) }
    }

    /** Verifies that upserting an equipment user makes it retrievable and triggers a notification for the users that can read equipment users. */
    @Test
    fun crudEquipmentUsers() = runServerTest {
        val equipmentUserUseCases = dependency.get()
            .useCases
            .equipmentUserUseCases
        val notificationsUseCases = dependency.get()
            .useCases
            .notificationsUseCases
        val lastSyncUtc = now() - 1.days

        assertTrue(actual = notificationsUseCases.observeNotifications(userUuid = FakeData.adminUser.uuid).firstOrNull().orEmpty().isEmpty())
        assertTrue(actual = equipmentUserUseCases.getEquipmentUsers(lastSyncUtc = lastSyncUtc, cursorUtc = null, cursorUuid = null, pageSize = ServerConfigs.value.pageSize).successData().isEmpty())
        assertEquals(expected = Outcome.Success(data = Unit), actual = equipmentUserUseCases.upsertEquipmentUser(equipmentUser = FakeData.equipmentUser))
        assertEquals(
            expected = listOf(element = FakeData.equipmentUser),
            actual = equipmentUserUseCases.getEquipmentUsers(lastSyncUtc = lastSyncUtc, cursorUtc = null, cursorUuid = null, pageSize = ServerConfigs.value.pageSize).successData()
        )
        assertTrue(actual = notificationsUseCases.observeNotifications(userUuid = FakeData.adminUser.uuid).firstOrNull().orEmpty().isNotEmpty())
    }

    /** Verifies that deleting and relinking the same pair keeps a single equipment user, with the latest change. */
    @Test
    fun relinkingReusesTheLink() = runServerTest {
        val equipmentUserUseCases = dependency.get()
            .useCases
            .equipmentUserUseCases
        val link = FakeData.equipmentUser

        equipmentUserUseCases.upsertEquipmentUser(equipmentUser = link)
        equipmentUserUseCases.upsertEquipmentUser(equipmentUser = link.copy(modifiedAt = link.modifiedAt + 1.seconds, deletedAt = link.modifiedAt + 1.seconds))
        val relinked = link.copy(modifiedAt = link.modifiedAt + 2.seconds)
        equipmentUserUseCases.upsertEquipmentUser(equipmentUser = relinked)

        val links = equipmentUserUseCases.getEquipmentUsers(lastSyncUtc = Instant.UNIX_EPOCH, cursorUtc = null, cursorUuid = null, pageSize = ServerConfigs.value.pageSize).successData()
        assertEquals(expected = listOf(element = relinked), actual = links)
    }

    /** Verifies that fetching a single equipment user returns it, and not found for an unknown uuid. */
    @Test
    fun getEquipmentUser() = runServerTest {
        val equipmentUserUseCases = dependency.get()
            .useCases
            .equipmentUserUseCases

        assertEquals(expected = Outcome.Success(data = Unit), actual = equipmentUserUseCases.upsertEquipmentUser(equipmentUser = FakeData.equipmentUser))
        assertEquals(expected = FakeData.equipmentUser, actual = equipmentUserUseCases.getEquipmentUser(uuid = FakeData.equipmentUser.uuid).successData())
        assertEquals(expected = Outcome.NotFound, actual = equipmentUserUseCases.getEquipmentUser(uuid = uuid()))
    }

    /** Verifies that when the database fails, its reads and writes report [Outcome.Error]. */
    @Test
    fun databaseFailureIsAnError() = runServerTest {
        val equipmentUserUseCases = dependency.get()
            .useCases
            .equipmentUserUseCases
        dependency.get().database.reset()

        listOf(
            "getEquipmentUsers" to equipmentUserUseCases.getEquipmentUsers(lastSyncUtc = Instant.UNIX_EPOCH, cursorUtc = null, cursorUuid = null, pageSize = ServerConfigs.value.pageSize),
            "getEquipmentUser" to equipmentUserUseCases.getEquipmentUser(uuid = FakeData.equipmentUser.uuid),
            "upsertEquipmentUser" to equipmentUserUseCases.upsertEquipmentUser(equipmentUser = FakeData.equipmentUser)
        ).forEach { (name, outcome) -> assertEquals(expected = Outcome.Error, actual = outcome, message = name) }
    }
}
