package com.app.builder.domain.usecase.seed

import kotlinx.serialization.Serializable
import com.app.builder.domain.Building
import com.app.builder.domain.BuildingEquipment
import com.app.builder.domain.BuildingMaterial
import com.app.builder.domain.BuildingUser
import com.app.builder.domain.Equipment
import com.app.builder.domain.EquipmentUser
import com.app.builder.domain.Material
import com.app.builder.domain.RegistrationForm
import com.app.builder.domain.Registry
import com.app.builder.domain.Task
import com.app.builder.domain.User

/**
 * The content of a seed file.
 *
 * @property users The users without an account.
 * @property registrations The users with an account, each with the account and its login password.
 * @property tasks The tasks.
 * @property equipments The equipments.
 * @property buildings The buildings.
 * @property materials The materials.
 * @property buildingEquipments The links between buildings and equipments.
 * @property buildingMaterials The links between buildings and materials.
 * @property buildingUsers The links between buildings and users.
 * @property equipmentUsers The links between equipments and users.
 * @property registries The registry entries.
 */
@Serializable
data class Seed(
    val users: List<User> = emptyList(),
    val registrations: List<RegistrationForm> = emptyList(),
    val tasks: List<Task> = emptyList(),
    val equipments: List<Equipment> = emptyList(),
    val buildings: List<Building> = emptyList(),
    val materials: List<Material> = emptyList(),
    val buildingEquipments: List<BuildingEquipment> = emptyList(),
    val buildingMaterials: List<BuildingMaterial> = emptyList(),
    val buildingUsers: List<BuildingUser> = emptyList(),
    val equipmentUsers: List<EquipmentUser> = emptyList(),
    val registries: List<Registry> = emptyList()
)
