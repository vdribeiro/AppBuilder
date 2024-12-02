package com.app.builder.domain.gateway

import io.ktor.client.HttpClient
import com.app.builder.domain.User
import com.app.builder.domain.gateway.authentication.AuthenticationGateway
import com.app.builder.domain.gateway.authentication.AuthenticationRepository
import com.app.builder.domain.gateway.authentication.AuthenticationUseCases
import com.app.builder.domain.gateway.config.ConfigGateway
import com.app.builder.domain.gateway.config.ConfigRepository
import com.app.builder.domain.gateway.config.ConfigUseCases
import com.app.builder.domain.gateway.devicelocation.DeviceLocationGateway
import com.app.builder.domain.gateway.devicelocation.DeviceLocationRepository
import com.app.builder.domain.gateway.push.PushGateway
import com.app.builder.domain.gateway.push.PushUseCases
import com.app.builder.domain.gateway.registry.RegistryGateway
import com.app.builder.domain.gateway.registry.RegistryRepository
import com.app.builder.domain.gateway.registry.RegistryUseCases
import com.app.builder.domain.gateway.task.TaskGateway
import com.app.builder.domain.gateway.task.TaskRepository
import com.app.builder.domain.gateway.task.TaskUseCases
import com.app.builder.domain.gateway.translation.TranslationGateway
import com.app.builder.domain.gateway.translation.TranslationRepository
import com.app.builder.domain.gateway.user.UserGateway
import com.app.builder.domain.gateway.user.UserRepository
import com.app.builder.domain.gateway.user.UserUseCases
import com.app.builder.domain.scheduler.Scheduler
import database.AppDatabase

/**
 * Gateways.
 *
 * @param user The user tied with the gateways.
 * @param database The SQLite database instance.
 * @param httpClient The HTTP client used for network operations.
 * @param scheduler The job scheduler for background tasks.
 */
class Gateways(
    user: User,
    database: AppDatabase,
    httpClient: HttpClient,
    scheduler: Scheduler
): UseCases, Repositories {

    private val translationGateway: TranslationGateway = TranslationGateway(
        database = database,
        httpClient = httpClient,
        scheduler = scheduler
    )
    override val translationRepository: TranslationRepository = translationGateway

    private val pushGateway: PushGateway = PushGateway(
        httpClient = httpClient
    )
    override val pushUseCases: PushUseCases = pushGateway

    private val deviceLocationGateway: DeviceLocationGateway = DeviceLocationGateway(
        user = user,
        database = database,
        httpClient = httpClient,
        scheduler = scheduler
    )
    override val deviceLocationRepository: DeviceLocationRepository = deviceLocationGateway

    private val configGateway: ConfigGateway = ConfigGateway(
        user = user,
        httpClient = httpClient,
        scheduler = scheduler
    )
    override val configUseCases: ConfigUseCases = configGateway
    override val configRepository: ConfigRepository = configGateway

    private val registryGateway: RegistryGateway = RegistryGateway(
        user = user,
        database = database,
        httpClient = httpClient,
        scheduler = scheduler
    )
    override val registryUseCases: RegistryUseCases = registryGateway
    override val registryRepository: RegistryRepository = registryGateway

    private val authenticationGateway: AuthenticationGateway = AuthenticationGateway(
        user = user,
        database = database,
        httpClient = httpClient,
        scheduler = scheduler
    )
    override val authenticationUseCases: AuthenticationUseCases = authenticationGateway
    override val authenticationRepository: AuthenticationRepository = authenticationGateway

    private val userGateway: UserGateway = UserGateway(
        user = user,
        database = database,
        httpClient = httpClient,
        scheduler = scheduler
    )
    override val userUseCases: UserUseCases = userGateway
    override val userRepository: UserRepository = userGateway

    private val taskGateway: TaskGateway = TaskGateway(
        user = user,
        database = database,
        httpClient = httpClient,
        scheduler = scheduler
    )
    override val taskUseCases: TaskUseCases = taskGateway
    override val taskRepository: TaskRepository = taskGateway
}
