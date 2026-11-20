package com.app.builder.domain.gateway.buildinguser

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

class BuildingUserUseCasesTest: TestCase() {

    /**
     * Gets the admin's use cases with [FakeData.building] and [FakeData.user] stored.
     *
     * @return The admin's [UseCases].
     */
    private suspend fun storedUseCases(): UseCases {
        val useCases = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
            .useCases
        useCases.buildingUseCases.upsertBuilding(building = FakeData.building)
        useCases.userUseCases.upsertUser(user = FakeData.user)
        return useCases
    }

    /** Verifies that a building user makes the building and the user observable from each other, and deleting it hides both. */
    @Test
    fun linkAndUnlink() = runUnitTest {
        val useCases = storedUseCases()
        val buildingUserUseCases = useCases.buildingUserUseCases

        assertTrue(actual = buildingUserUseCases.observeUsersInBuilding(buildingUuid = FakeData.building.uuid).firstOrNull().orEmpty().isEmpty())
        buildingUserUseCases.upsertBuildingUser(buildingUser = FakeData.buildingUser)
        assertEquals(expected = listOf(element = FakeData.user), actual = buildingUserUseCases.observeUsersInBuilding(buildingUuid = FakeData.building.uuid).firstOrNull())
        assertEquals(expected = listOf(element = FakeData.building), actual = buildingUserUseCases.observeBuildingsOfUser(userUuid = FakeData.user.uuid).firstOrNull())

        buildingUserUseCases.upsertBuildingUser(buildingUser = FakeData.buildingUser.copy(modifiedAt = now(), deletedAt = now()))
        assertTrue(actual = buildingUserUseCases.observeUsersInBuilding(buildingUuid = FakeData.building.uuid).firstOrNull().orEmpty().isEmpty())
        assertTrue(actual = buildingUserUseCases.observeBuildingsOfUser(userUuid = FakeData.user.uuid).firstOrNull().orEmpty().isEmpty())
    }

    /** Verifies that deleted users and buildings are left out even while they are still linked. */
    @Test
    fun deletedEntitiesAreLeftOut() = runUnitTest {
        val useCases = storedUseCases()
        val buildingUserUseCases = useCases.buildingUserUseCases
        val deletedUser = FakeData.user.copy(uuid = uuid(), deletedAt = now())
        useCases.userUseCases.upsertUser(user = deletedUser)

        buildingUserUseCases.upsertBuildingUser(buildingUser = FakeData.buildingUser)
        buildingUserUseCases.upsertBuildingUser(buildingUser = FakeData.buildingUser.copy(userUuid = deletedUser.uuid))
        assertEquals(expected = listOf(element = FakeData.user), actual = buildingUserUseCases.observeUsersInBuilding(buildingUuid = FakeData.building.uuid).firstOrNull())

        useCases.buildingUseCases.upsertBuilding(building = FakeData.building.copy(deletedAt = now()))
        assertTrue(actual = buildingUserUseCases.observeBuildingsOfUser(userUuid = FakeData.user.uuid).firstOrNull().orEmpty().isEmpty())
    }

    /** Verifies that an account with building user read permission sees building users, and cannot change them. */
    @Test
    fun readOnlyAccountCannotLink() = runUnitTest {
        storedUseCases().buildingUserUseCases.upsertBuildingUser(buildingUser = FakeData.buildingUser)
        val readerUseCases = dependency.get()
            .getUserDependency(account = FakeData.account)
            .useCases
            .buildingUserUseCases

        assertEquals(expected = listOf(element = FakeData.user), actual = readerUseCases.observeUsersInBuilding(buildingUuid = FakeData.building.uuid).firstOrNull())
        readerUseCases.upsertBuildingUser(buildingUser = FakeData.buildingUser.copy(modifiedAt = now(), deletedAt = now()))
        assertEquals(expected = listOf(element = FakeData.user), actual = readerUseCases.observeUsersInBuilding(buildingUuid = FakeData.building.uuid).firstOrNull())
    }
}
