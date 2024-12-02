package com.app.builder.domain.gateway

import com.app.builder.domain.gateway.authentication.AuthenticationRepository
import com.app.builder.domain.gateway.config.ConfigRepository
import com.app.builder.domain.gateway.devicelocation.DeviceLocationRepository
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
    /**The repository for authentication, registration, and session management. */
    val authenticationRepository: AuthenticationRepository
    /**The repository for managing users. */
    val userRepository: UserRepository
    /**The repository for managing tasks. */
    val taskRepository: TaskRepository
}
