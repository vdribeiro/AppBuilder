package com.app.builder.domain.gateway.buildinguser

import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow
import com.app.builder.domain.Building
import com.app.builder.domain.BuildingUser
import com.app.builder.domain.User

/** Defines the business workflows for building users. */
interface BuildingUserUseCases {

    /**
     * Observes the users linked to a building, leaving out deleted users and deleted building users.
     *
     * @param buildingUuid The unique identifier of the building.
     * @return [Flow] emitting the current list of [User] linked to the building.
     */
    fun observeUsersInBuilding(buildingUuid: Uuid): Flow<List<User>>

    /**
     * Observes the buildings linked to a user, leaving out deleted buildings and deleted building users.
     *
     * @param userUuid The unique identifier of the user.
     * @return [Flow] emitting the current list of [Building] linked to the user.
     */
    fun observeBuildingsOfUser(userUuid: Uuid): Flow<List<Building>>

    /**
     * Inserts a new building user or updates an existing one.
     *
     * @param buildingUser The [BuildingUser] to be inserted or updated.
     */
    suspend fun upsertBuildingUser(buildingUser: BuildingUser)
}
