package com.app.builder.test

import kotlin.uuid.Uuid
import com.app.builder.core.config.ClientConfigs
import com.app.builder.core.config.ClientFlags
import com.app.builder.core.config.ServerConfigs
import com.app.builder.core.config.ServerFlags
import com.app.builder.core.locale.now
import com.app.builder.core.platform.OS
import com.app.builder.core.security.uuid
import com.app.builder.data.serializer.encode
import com.app.builder.domain.DeviceLocation
import com.app.builder.domain.EntityType
import com.app.builder.domain.Permission
import com.app.builder.domain.PushPayload
import com.app.builder.domain.Registry
import com.app.builder.domain.Task
import com.app.builder.domain.Translation
import com.app.builder.domain.User
import com.app.builder.domain.RegistrationForm
import com.app.builder.domain.UserCredentials
import com.app.builder.domain.Authentication
import com.app.builder.domain.BearerToken

/** A collection of fake data for testing. */
object FakeData {

    val clientFlags = ClientFlags.flags.copy()

    val clientConfigs = ClientConfigs.configs.copy()

    val serverFlags: ServerFlags = ServerFlags.flags.copy()

    val serverConfigs: ServerConfigs = ServerConfigs.configs.copy()

    val translations: List<Translation> = listOf(
        Translation(
            languageIso = "en",
            key = "key",
            value = "value"
        ),
        Translation(
            languageIso = "en",
            key = "hello_world",
            value = "Hello World!"
        ),
        Translation(
            languageIso = "pt",
            key = "chave",
            value = "valor"
        ),
    )

    val adminUser: User = User(
        uuid = Uuid.NIL,
        modifiedAt = now(),
        deletedAt = null,
        permissions = EntityType.entries.associateWith { Permission.WRITE },
        name = "Admin",
        avatar = "The Last Airbender"
    )

    val user: User = User(
        uuid = uuid(),
        modifiedAt = now(),
        deletedAt = null,
        permissions = EntityType.entries.mapNotNull { type ->
            when (type) {
                EntityType.CLIENT_FLAG -> Permission.READ
                EntityType.CLIENT_CONFIG -> Permission.READ
                EntityType.SERVER_FLAG -> null
                EntityType.SERVER_CONFIG -> null
                EntityType.REGISTRY -> Permission.READ
                EntityType.NOTIFICATION -> null
                EntityType.TRANSLATION -> Permission.READ
                EntityType.SESSION -> Permission.READ
                EntityType.USER -> Permission.WRITE
                EntityType.DEVICE_LOCATION -> Permission.WRITE
                EntityType.TASK -> Permission.READ
                EntityType.FILE -> Permission.READ
            }?.let { type to it }
        }.toMap(),
        name = "User",
        avatar = null
    )

    val adminBearerToken: BearerToken = BearerToken(
        accessToken = "adminAccessToken",
        refreshToken = "adminRefreshToken"
    )

    val bearerToken: BearerToken = BearerToken(
        accessToken = "accessToken",
        refreshToken = "refreshToken"
    )

    val adminCredentials = UserCredentials(
        username = "admin",
        password = "admin"
    )

    val credentials = UserCredentials(
        username = "user",
        password = "pass123"
    )

    val adminRegistrationForm = RegistrationForm(
        user = adminUser,
        credentials = adminCredentials
    )

    val registrationForm = RegistrationForm(
        user = user,
        credentials = credentials
    )

    val adminAuthentication: Authentication = Authentication(
        user = adminUser,
        bearer = adminBearerToken
    )

    val authentication: Authentication = Authentication(
        user = user,
        bearer = bearerToken
    )

    val notification: PushPayload.Notification = PushPayload.Notification(
        uuid = uuid(),
        userUuid = adminUser.uuid,
        title = "Notification Title",
        description = "Notification Description",
    )

    val entityNotification: PushPayload.EntityNotification = PushPayload.EntityNotification(
        uuid = uuid(),
        userUuid = adminUser.uuid,
        title = "Entity Notification Title",
        description = "Entity Notification Description",
        entityType = EntityType.TRANSLATION
    )

    val registry: Registry = Registry(
        requestUuid = uuid(),
        requestType = "POST",
        requestUtc = now(),
        requestSentUtc = now(),
        deletedAt = null,
        userUuid = adminUser.uuid,
        appVersion = "1.0.0",
        os = OS.Android,
        osVersion = "14",
        brand = "Google",
        model = "Pixel 8",
        deviceUuid = uuid(),
        entityUuid = user.uuid,
        entityType = EntityType.USER,
        payload = encode(value = user)
    )

    val task: Task = Task(
        uuid = uuid(),
        modifiedAt = now(),
        deletedAt = null,
        title = "Task Title",
        description = "Task Description",
        state = Task.State.TODO
    )

    val deviceLocation: DeviceLocation = DeviceLocation(
        uuid = uuid(),
        userUuid = adminUser.uuid,
        provider = "gps",
        fixTime = now(),
        deviceTime = now(),
        latitude = 38.7223,
        longitude = -9.1393,
        altitude = 100.0,
        accuracy = 5.0,
        bearing = 90.0,
        speed = 1.5,
    )
}