package com.app.builder.domain.gateway.equipmentuser

import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow
import com.app.builder.domain.Equipment
import com.app.builder.domain.EquipmentUser
import com.app.builder.domain.User

/** Defines the business workflows for equipment users. */
interface EquipmentUserUseCases {

    /**
     * Observes the users linked to an equipment, leaving out deleted users and deleted equipment users.
     *
     * @param equipmentUuid The unique identifier of the equipment.
     * @return [Flow] emitting the current list of [User] linked to the equipment.
     */
    fun observeUsersOfEquipment(equipmentUuid: Uuid): Flow<List<User>>

    /**
     * Observes the equipments linked to a user, leaving out deleted equipments and deleted equipment users.
     *
     * @param userUuid The unique identifier of the user.
     * @return [Flow] emitting the current list of [Equipment] linked to the user.
     */
    fun observeEquipmentsOfUser(userUuid: Uuid): Flow<List<Equipment>>

    /**
     * Inserts a new equipment user or updates an existing one.
     *
     * @param equipmentUser The [EquipmentUser] to be inserted or updated.
     */
    suspend fun upsertEquipmentUser(equipmentUser: EquipmentUser)
}
