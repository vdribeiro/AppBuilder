package com.app.builder.domain.usecase

import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import com.app.builder.data.signal.InstanceSignal
import com.app.builder.domain.usecase.authentication.AuthenticationGateway
import com.app.builder.domain.usecase.authentication.AuthenticationUseCases
import com.app.builder.domain.usecase.config.ConfigGateway
import com.app.builder.domain.usecase.config.ConfigUseCases
import com.app.builder.domain.usecase.connection.ConnectionGateway
import com.app.builder.domain.usecase.connection.ConnectionUseCases
import com.app.builder.domain.usecase.devicelocation.DeviceLocationGateway
import com.app.builder.domain.usecase.devicelocation.DeviceLocationUseCases
import com.app.builder.domain.usecase.devicetoken.DeviceTokenGateway
import com.app.builder.domain.usecase.devicetoken.DeviceTokenUseCases
import com.app.builder.domain.usecase.notification.NotificationsGateway
import com.app.builder.domain.usecase.notification.NotificationsUseCases
import com.app.builder.domain.usecase.registry.RegistryGateway
import com.app.builder.domain.usecase.registry.RegistryUseCases
import com.app.builder.domain.usecase.task.TaskGateway
import com.app.builder.domain.usecase.task.TaskUseCases
import com.app.builder.domain.usecase.ticket.TicketGateway
import com.app.builder.domain.usecase.ticket.TicketUseCases
import com.app.builder.domain.usecase.user.UserGateway
import com.app.builder.domain.usecase.user.UserUseCases

/**
 * Composes the concrete gateway implementation of every [UseCases] property, wiring each one to the shared [database] and, where needed, [instanceSignal].
 *
 * @param database The reactive database instance shared by every gateway.
 * @param instanceSignal The pub/sub bus shared by gateways that need to signal across server instances.
 */
class Gateways(
    database: R2dbcDatabase,
    instanceSignal: InstanceSignal
): UseCases {
    override val configUseCases: ConfigUseCases = ConfigGateway(database = database)
    override val authenticationUseCases: AuthenticationUseCases = AuthenticationGateway(database = database)
    override val registryUseCases: RegistryUseCases = RegistryGateway(database = database)
    override val deviceTokenUseCases: DeviceTokenUseCases = DeviceTokenGateway(database = database)
    override val ticketUseCases: TicketUseCases = TicketGateway(database = database)
    override val connectionUseCases: ConnectionUseCases = ConnectionGateway(database = database)
    override val notificationsUseCases: NotificationsUseCases = NotificationsGateway(database = database, instanceSignal = instanceSignal)
    override val userUseCases: UserUseCases = UserGateway(database = database)
    override val deviceLocationUseCases: DeviceLocationUseCases = DeviceLocationGateway(database = database)
    override val taskUseCases: TaskUseCases = TaskGateway(database = database, notificationsUseCases = notificationsUseCases)
}
