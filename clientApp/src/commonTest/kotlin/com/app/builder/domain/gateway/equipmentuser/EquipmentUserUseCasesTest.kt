package com.app.builder.domain.gateway.equipmentuser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.firstOrNull
import com.app.builder.Dependency.getUserDependency
import com.app.builder.core.locale.now
import com.app.builder.core.security.uuid
import com.app.builder.domain.gateway.UseCases
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class EquipmentUserUseCasesTest: TestCase() {

    /**
     * Gets the admin's use cases with [FakeData.equipment] and [FakeData.user] stored.
     *
     * @return The admin's [UseCases].
     */
    private suspend fun storedUseCases(): UseCases {
        val useCases = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
            .useCases
        useCases.equipmentUseCases.upsertEquipment(equipment = FakeData.equipment)
        useCases.userUseCases.upsertUser(user = FakeData.user)
        return useCases
    }

    /** Verifies that an equipment user makes the equipment and the user observable from each other, and deleting it hides both. */
    @Test
    fun linkAndUnlink() = runUnitTest {
        val useCases = storedUseCases()
        val equipmentUserUseCases = useCases.equipmentUserUseCases

        assertTrue(actual = equipmentUserUseCases.observeUsersOfEquipment(equipmentUuid = FakeData.equipment.uuid).firstOrNull().orEmpty().isEmpty())
        equipmentUserUseCases.upsertEquipmentUser(equipmentUser = FakeData.equipmentUser)
        assertEquals(expected = listOf(element = FakeData.user), actual = equipmentUserUseCases.observeUsersOfEquipment(equipmentUuid = FakeData.equipment.uuid).firstOrNull())
        assertEquals(expected = listOf(element = FakeData.equipment), actual = equipmentUserUseCases.observeEquipmentsOfUser(userUuid = FakeData.user.uuid).firstOrNull())

        equipmentUserUseCases.upsertEquipmentUser(equipmentUser = FakeData.equipmentUser.copy(modifiedAt = now(), deletedAt = now()))
        assertTrue(actual = equipmentUserUseCases.observeUsersOfEquipment(equipmentUuid = FakeData.equipment.uuid).firstOrNull().orEmpty().isEmpty())
        assertTrue(actual = equipmentUserUseCases.observeEquipmentsOfUser(userUuid = FakeData.user.uuid).firstOrNull().orEmpty().isEmpty())
    }

    /** Verifies that deleted users and equipments are left out even while they are still linked. */
    @Test
    fun deletedEntitiesAreLeftOut() = runUnitTest {
        val useCases = storedUseCases()
        val equipmentUserUseCases = useCases.equipmentUserUseCases
        val deletedUser = FakeData.user.copy(uuid = uuid(), deletedAt = now())
        useCases.userUseCases.upsertUser(user = deletedUser)

        equipmentUserUseCases.upsertEquipmentUser(equipmentUser = FakeData.equipmentUser)
        equipmentUserUseCases.upsertEquipmentUser(equipmentUser = FakeData.equipmentUser.copy(userUuid = deletedUser.uuid))
        assertEquals(expected = listOf(element = FakeData.user), actual = equipmentUserUseCases.observeUsersOfEquipment(equipmentUuid = FakeData.equipment.uuid).firstOrNull())

        useCases.equipmentUseCases.upsertEquipment(equipment = FakeData.equipment.copy(deletedAt = now()))
        assertTrue(actual = equipmentUserUseCases.observeEquipmentsOfUser(userUuid = FakeData.user.uuid).firstOrNull().orEmpty().isEmpty())
    }

    /** Verifies that an account with equipment user read permission sees equipment users, and cannot change them. */
    @Test
    fun readOnlyAccountCannotLink() = runUnitTest {
        storedUseCases().equipmentUserUseCases.upsertEquipmentUser(equipmentUser = FakeData.equipmentUser)
        val readerUseCases = dependency.get()
            .getUserDependency(account = FakeData.account)
            .useCases
            .equipmentUserUseCases

        assertEquals(expected = listOf(element = FakeData.user), actual = readerUseCases.observeUsersOfEquipment(equipmentUuid = FakeData.equipment.uuid).firstOrNull())
        readerUseCases.upsertEquipmentUser(equipmentUser = FakeData.equipmentUser.copy(modifiedAt = now(), deletedAt = now()))
        assertEquals(expected = listOf(element = FakeData.user), actual = readerUseCases.observeUsersOfEquipment(equipmentUuid = FakeData.equipment.uuid).firstOrNull())
    }
}
