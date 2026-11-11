package com.app.builder.test

import com.app.builder.core.config.ClientConfigs
import com.app.builder.core.config.ClientFlags
import com.app.builder.core.config.ServerConfigs
import com.app.builder.core.config.ServerFlags
import com.app.builder.core.locale.now
import com.app.builder.core.platform.OS
import com.app.builder.core.security.uuid
import com.app.builder.data.serializer.encode
import com.app.builder.domain.Account
import com.app.builder.domain.Authentication
import com.app.builder.domain.BearerToken
import com.app.builder.domain.DeviceLocation
import com.app.builder.domain.EntityType
import com.app.builder.domain.Equipment
import com.app.builder.domain.Building
import com.app.builder.domain.Permission
import com.app.builder.domain.PushPayload
import com.app.builder.domain.RegistrationForm
import com.app.builder.domain.Registry
import com.app.builder.domain.Task
import com.app.builder.domain.Translation
import com.app.builder.domain.User
import com.app.builder.domain.UserCredentials

/** A collection of fake data for testing. */
object FakeData {

    /** The application id the test suites run under, kept apart from the real app's. */
    const val APP_ID = "com.app.builder.test"

    /** The application name the test suites run under, which also names their data directory. */
    const val APP_NAME = "AppBuilder Test"

    /** The application version the test suites run under. */
    const val APP_VERSION = "0.0.0"

    val clientFlags = ClientFlags.value.copy()

    val clientConfigs = ClientConfigs.value.copy()

    val serverFlags: ServerFlags = ServerFlags.value.copy()

    val serverConfigs: ServerConfigs = ServerConfigs.value.copy()

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
        name = "Admin",
        avatar = "The Last Airbender"
    )

    val user: User = User(
        name = "User"
    )

    val adminAccount: Account = Account(
        userUuid = adminUser.uuid,
        username = "admin",
        permissions = EntityType.entries.associateWith { Permission.WRITE }
    )

    val account: Account = Account(
        userUuid = user.uuid,
        username = "user",
        permissions = EntityType.entries.mapNotNull { type ->
            when (type) {
                EntityType.CLIENT_FLAG -> Permission.READ
                EntityType.CLIENT_CONFIG -> Permission.READ
                EntityType.SERVER_FLAG -> null
                EntityType.SERVER_CONFIG -> null
                EntityType.REGISTRY -> null
                EntityType.NOTIFICATION -> null
                EntityType.TRANSLATION -> Permission.READ
                EntityType.ACCOUNT -> Permission.READ
                EntityType.USER -> Permission.READ
                EntityType.DEVICE_LOCATION -> Permission.WRITE
                EntityType.NFC -> Permission.READ
                EntityType.TASK -> Permission.READ
                EntityType.EQUIPMENT -> Permission.READ
                EntityType.BUILDING -> Permission.READ
            }?.let { type to it }
        }.toMap()
    )

    val adminBearerToken: BearerToken = BearerToken(
        accessToken = "adminAccessToken",
        refreshToken = "adminRefreshToken"
    )

    val bearerToken: BearerToken = BearerToken(
        accessToken = "accessToken",
        refreshToken = "refreshToken"
    )

    val expiredBearerToken: BearerToken = BearerToken(
        accessToken = "expiredAccessToken",
        refreshToken = "rejectedRefreshToken"
    )

    val adminCredentials = UserCredentials(
        username = adminAccount.username,
        password = "admin"
    )

    val credentials = UserCredentials(
        username = account.username,
        password = "pass123"
    )

    val adminRegistrationForm = RegistrationForm(
        user = adminUser,
        account = adminAccount,
        password = adminCredentials.password
    )

    val registrationForm = RegistrationForm(
        user = user,
        account = account,
        password = credentials.password
    )

    val adminAuthentication: Authentication = Authentication(
        user = adminUser,
        account = adminAccount,
        bearer = adminBearerToken
    )

    val authentication: Authentication = Authentication(
        user = user,
        account = account,
        bearer = bearerToken
    )

    val notification: PushPayload.Notification = PushPayload.Notification(
        userUuid = adminUser.uuid,
        title = "Notification Title",
        description = "Notification Description",
    )

    val entityNotification: PushPayload.EntityNotification = PushPayload.EntityNotification(
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
        title = "Task Title",
        description = "Task Description"
    )

    val equipment: Equipment = Equipment(
        name = "Equipment Name",
        description = "Equipment Description"
    )

    val building: Building = Building(
        name = "Building Name",
        description = "Building Description",
        address = "Building Address",
        latitude = 41.1579,
        longitude = -8.6291
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