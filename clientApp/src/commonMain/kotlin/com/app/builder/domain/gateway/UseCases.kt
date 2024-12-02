package com.app.builder.domain.gateway

import com.app.builder.domain.gateway.authentication.AuthenticationUseCases
import com.app.builder.domain.gateway.config.ConfigUseCases
import com.app.builder.domain.gateway.push.PushUseCases
import com.app.builder.domain.gateway.registry.RegistryUseCases
import com.app.builder.domain.gateway.task.TaskUseCases
import com.app.builder.domain.gateway.user.UserUseCases

/** A central interface that groups together all the use cases of the application. */
interface UseCases {
    /** The use cases for sending push notifications. */
    val pushUseCases: PushUseCases
    /** The use cases for managing feature flags and remote configs. */
    val configUseCases: ConfigUseCases
    /** The use cases for managing registries. */
    val registryUseCases: RegistryUseCases
    /** The use cases for authentication, registration, and session management. */
    val authenticationUseCases: AuthenticationUseCases
    /** The use cases for managing users. */
    val userUseCases: UserUseCases
    /** The use cases for managing tasks. */
    val taskUseCases: TaskUseCases
}
