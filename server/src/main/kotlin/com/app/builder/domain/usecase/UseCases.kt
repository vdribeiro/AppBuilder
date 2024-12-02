package com.app.builder.domain.usecase

import com.app.builder.domain.usecase.authentication.AuthenticationUseCases
import com.app.builder.domain.usecase.config.ConfigUseCases
import com.app.builder.domain.usecase.connection.ConnectionUseCases
import com.app.builder.domain.usecase.devicelocation.DeviceLocationUseCases
import com.app.builder.domain.usecase.devicetoken.DeviceTokenUseCases
import com.app.builder.domain.usecase.notification.NotificationsUseCases
import com.app.builder.domain.usecase.registry.RegistryUseCases
import com.app.builder.domain.usecase.task.TaskUseCases
import com.app.builder.domain.usecase.ticket.TicketUseCases
import com.app.builder.domain.usecase.user.UserUseCases

/** A central interface that groups together all the use cases of the application. */
interface UseCases {
    /** The feature flags and configs use cases. */
    val configUseCases: ConfigUseCases
    /** The user authentication, registration, and session use cases. */
    val authenticationUseCases: AuthenticationUseCases
    /** The request registry use cases. */
    val registryUseCases: RegistryUseCases
    /** The device push token use cases. */
    val deviceTokenUseCases: DeviceTokenUseCases
    /** The single-use, short-lived push connection ticket use cases. */
    val ticketUseCases: TicketUseCases
    /** The WebSocket connection presence use cases. */
    val connectionUseCases: ConnectionUseCases
    /** The notification use cases. */
    val notificationsUseCases: NotificationsUseCases
    /** The user use cases. */
    val userUseCases: UserUseCases
    /** The device location use cases. */
    val deviceLocationUseCases: DeviceLocationUseCases
    /** The task use cases. */
    val taskUseCases: TaskUseCases
}
