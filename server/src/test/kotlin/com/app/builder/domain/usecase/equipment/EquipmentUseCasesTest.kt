package com.app.builder.domain.usecase.equipment

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import kotlinx.coroutines.flow.firstOrNull
import com.app.builder.core.config.ServerConfigs
import com.app.builder.core.locale.now
import com.app.builder.core.security.uuid
import com.app.builder.data.database.reset
import com.app.builder.domain.usecase.Outcome
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class EquipmentUseCasesTest: TestCase() {

    /** Verifies that upserting an equipment makes it retrievable and triggers a notification for the users that can read equipments. */
    @Test
    fun crudEquipments() = runServerTest {
        val equipmentUseCases = dependency.get()
            .useCases
            .equipmentUseCases
        val notificationsUseCases = dependency.get()
            .useCases
            .notificationsUseCases
        val lastSyncUtc = now() - 1.days

        assertTrue(actual = notificationsUseCases.observeNotifications(userUuid = FakeData.adminUser.uuid).firstOrNull().orEmpty().isEmpty())
        assertTrue(actual = equipmentUseCases.getEquipments(lastSyncUtc = lastSyncUtc, cursorUtc = null, cursorUuid = null, pageSize = ServerConfigs.value.pageSize).successData().isEmpty())
        assertEquals(expected = Outcome.Success(data = Unit), actual = equipmentUseCases.upsertEquipment(equipment = FakeData.equipment))
        assertEquals(expected = listOf(element = FakeData.equipment), actual = equipmentUseCases.getEquipments(lastSyncUtc = lastSyncUtc, cursorUtc = null, cursorUuid = null, pageSize = ServerConfigs.value.pageSize).successData())
        assertTrue(actual = notificationsUseCases.observeNotifications(userUuid = FakeData.adminUser.uuid).firstOrNull().orEmpty().isNotEmpty())
    }

    /** Verifies that fetching a single equipment returns it, and null for an unknown uuid. */
    @Test
    fun getEquipment() = runServerTest {
        val equipmentUseCases = dependency.get()
            .useCases
            .equipmentUseCases

        assertEquals(expected = Outcome.Success(data = Unit), actual = equipmentUseCases.upsertEquipment(equipment = FakeData.equipment))
        assertEquals(expected = FakeData.equipment, actual = equipmentUseCases.getEquipment(uuid = FakeData.equipment.uuid).successData())
        assertEquals(expected = Outcome.NotFound, actual = equipmentUseCases.getEquipment(uuid = uuid()))
    }

    /** Verifies that when the database fails, its reads and writes report [Outcome.Error] rather than an empty list, a missing equipment or a refused write. */
    @Test
    fun databaseFailureIsAnError() = runServerTest {
        val equipmentUseCases = dependency.get()
            .useCases
            .equipmentUseCases
        dependency.get().database.reset()

        listOf(
            "getEquipments" to equipmentUseCases.getEquipments(lastSyncUtc = Instant.fromEpochSeconds(epochSeconds = 0), cursorUtc = null, cursorUuid = null, pageSize = ServerConfigs.value.pageSize),
            "getEquipment" to equipmentUseCases.getEquipment(uuid = FakeData.equipment.uuid),
            "upsertEquipment" to equipmentUseCases.upsertEquipment(equipment = FakeData.equipment)
        ).forEach { (name, outcome) -> assertEquals(expected = Outcome.Error, actual = outcome, message = name) }
    }
}
