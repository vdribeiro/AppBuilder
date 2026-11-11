package com.app.builder.domain.gateway

import io.ktor.client.HttpClient
import com.app.builder.domain.Account
import com.app.builder.domain.clock.ClockService
import com.app.builder.domain.gateway.account.AccountGateway
import com.app.builder.domain.gateway.account.AccountRepository
import com.app.builder.domain.gateway.account.AccountUseCases
import com.app.builder.domain.gateway.authentication.AuthenticationGateway
import com.app.builder.domain.gateway.authentication.AuthenticationRepository
import com.app.builder.domain.gateway.authentication.AuthenticationUseCases
import com.app.builder.domain.gateway.building.BuildingGateway
import com.app.builder.domain.gateway.building.BuildingRepository
import com.app.builder.domain.gateway.building.BuildingUseCases
import com.app.builder.domain.gateway.buildingequipment.BuildingEquipmentGateway
import com.app.builder.domain.gateway.buildingequipment.BuildingEquipmentRepository
import com.app.builder.domain.gateway.buildingequipment.BuildingEquipmentUseCases
import com.app.builder.domain.gateway.config.ConfigGateway
import com.app.builder.domain.gateway.config.ConfigRepository
import com.app.builder.domain.gateway.config.ConfigUseCases
import com.app.builder.domain.gateway.devicelocation.DeviceLocationGateway
import com.app.builder.domain.gateway.devicelocation.DeviceLocationRepository
import com.app.builder.domain.gateway.equipment.EquipmentGateway
import com.app.builder.domain.gateway.equipment.EquipmentRepository
import com.app.builder.domain.gateway.equipment.EquipmentUseCases
import com.app.builder.domain.gateway.push.PushGateway
import com.app.builder.domain.gateway.push.PushUseCases
import com.app.builder.domain.gateway.registry.RegistryGateway
import com.app.builder.domain.gateway.registry.RegistryRepository
import com.app.builder.domain.gateway.registry.RegistryUseCases
import com.app.builder.domain.gateway.status.StatusGateway
import com.app.builder.domain.gateway.status.StatusUseCases
import com.app.builder.domain.gateway.task.TaskGateway
import com.app.builder.domain.gateway.task.TaskRepository
import com.app.builder.domain.gateway.task.TaskUseCases
import com.app.builder.domain.gateway.translation.TranslationGateway
import com.app.builder.domain.gateway.translation.TranslationRepository
import com.app.builder.domain.gateway.translation.TranslationUseCases
import com.app.builder.domain.gateway.user.UserGateway
import com.app.builder.domain.gateway.user.UserRepository
import com.app.builder.domain.gateway.user.UserUseCases
import com.app.builder.domain.scheduler.Scheduler
import database.AppDatabase

/**
 * Gateways.
 *
 * @param account The account of the user tied with the gateways.
 * @param database The SQLite database instance.
 * @param plainHttpClient The HTTP client without bearer token handling.
 * @param httpClient The HTTP client with bearer token handling.
 * @param scheduler The job scheduler for background tasks.
 * @param clockService The service managing the device's clock.
 */
class Gateways(
    account: Account,
    database: AppDatabase,
    plainHttpClient: HttpClient,
    httpClient: HttpClient,
    scheduler: Scheduler,
    clockService: ClockService
): UseCases, Repositories {

    private val translationGateway: TranslationGateway = TranslationGateway(
        database = database,
        httpClient = httpClient,
        scheduler = scheduler
    )
    override val translationUseCases: TranslationUseCases = translationGateway
    override val translationRepository: TranslationRepository = translationGateway

    private val pushGateway: PushGateway = PushGateway(
        httpClient = httpClient
    )
    override val pushUseCases: PushUseCases = pushGateway

    private val deviceLocationGateway: DeviceLocationGateway = DeviceLocationGateway(
        account = account,
        database = database,
        httpClient = httpClient,
        scheduler = scheduler
    )
    override val deviceLocationRepository: DeviceLocationRepository = deviceLocationGateway

    private val configGateway: ConfigGateway = ConfigGateway(
        account = account,
        httpClient = httpClient,
        scheduler = scheduler
    )
    override val configUseCases: ConfigUseCases = configGateway
    override val configRepository: ConfigRepository = configGateway

    private val registryGateway: RegistryGateway = RegistryGateway(
        account = account,
        database = database,
        httpClient = httpClient,
        scheduler = scheduler
    )
    override val registryUseCases: RegistryUseCases = registryGateway
    override val registryRepository: RegistryRepository = registryGateway

    private val authenticationGateway: AuthenticationGateway = AuthenticationGateway(
        database = database,
        httpClient = plainHttpClient,
        scheduler = scheduler
    )
    override val authenticationUseCases: AuthenticationUseCases = authenticationGateway
    override val authenticationRepository: AuthenticationRepository = authenticationGateway

    private val statusGateway: StatusGateway = StatusGateway(
        scheduler = scheduler,
        clockService = clockService
    )
    override val statusUseCases: StatusUseCases = statusGateway

    private val userGateway: UserGateway = UserGateway(
        account = account,
        database = database,
        httpClient = httpClient,
        scheduler = scheduler
    )
    override val userUseCases: UserUseCases = userGateway
    override val userRepository: UserRepository = userGateway

    private val accountGateway: AccountGateway = AccountGateway(
        account = account,
        database = database,
        httpClient = httpClient,
        scheduler = scheduler
    )
    override val accountUseCases: AccountUseCases = accountGateway
    override val accountRepository: AccountRepository = accountGateway

    private val taskGateway: TaskGateway = TaskGateway(
        account = account,
        database = database,
        httpClient = httpClient,
        scheduler = scheduler
    )
    override val taskUseCases: TaskUseCases = taskGateway
    override val taskRepository: TaskRepository = taskGateway

    private val equipmentGateway: EquipmentGateway = EquipmentGateway(
        account = account,
        database = database,
        httpClient = httpClient,
        scheduler = scheduler
    )
    override val equipmentUseCases: EquipmentUseCases = equipmentGateway
    override val equipmentRepository: EquipmentRepository = equipmentGateway

    private val buildingGateway: BuildingGateway = BuildingGateway(
        account = account,
        database = database,
        httpClient = httpClient,
        scheduler = scheduler
    )
    override val buildingUseCases: BuildingUseCases = buildingGateway
    override val buildingRepository: BuildingRepository = buildingGateway

    private val buildingEquipmentGateway: BuildingEquipmentGateway = BuildingEquipmentGateway(
        account = account,
        database = database,
        httpClient = httpClient,
        scheduler = scheduler
    )
    override val buildingEquipmentUseCases: BuildingEquipmentUseCases = buildingEquipmentGateway
    override val buildingEquipmentRepository: BuildingEquipmentRepository = buildingEquipmentGateway
}
