package com.app.builder.domain.gateway

import com.app.builder.domain.gateway.account.AccountRepository
import com.app.builder.domain.gateway.authentication.AuthenticationRepository
import com.app.builder.domain.gateway.building.BuildingRepository
import com.app.builder.domain.gateway.buildingequipment.BuildingEquipmentRepository
import com.app.builder.domain.gateway.config.ConfigRepository
import com.app.builder.domain.gateway.devicelocation.DeviceLocationRepository
import com.app.builder.domain.gateway.equipment.EquipmentRepository
import com.app.builder.domain.gateway.registry.RegistryRepository
import com.app.builder.domain.gateway.task.TaskRepository
import com.app.builder.domain.gateway.translation.TranslationRepository
import com.app.builder.domain.gateway.user.UserRepository

/** A central interface grouping the repositories. */
interface Repositories {
    /**The repository for managing translations. */
    val translationRepository: TranslationRepository
    /**The repository for delivering recorded device locations. */
    val deviceLocationRepository: DeviceLocationRepository
    /**The repository for managing feature flags and remote configs. */
    val configRepository: ConfigRepository
    /**The repository for managing registries. */
    val registryRepository: RegistryRepository
    /**The repository for authentication and session management. */
    val authenticationRepository: AuthenticationRepository
    /**The repository for managing users. */
    val userRepository: UserRepository
    /**The repository for managing accounts. */
    val accountRepository: AccountRepository
    /**The repository for managing tasks. */
    val taskRepository: TaskRepository
    /**The repository for managing equipments. */
    val equipmentRepository: EquipmentRepository
    /**The repository for managing buildings. */
    val buildingRepository: BuildingRepository
    /** The repository for managing building equipments. */
    val buildingEquipmentRepository: BuildingEquipmentRepository
}
