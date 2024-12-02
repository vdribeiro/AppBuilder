package com.app.builder

import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.engine.addShutdownHook
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import com.app.builder.core.config.ClientConfigs
import com.app.builder.core.config.ClientFlags
import com.app.builder.core.config.ServerConfigs
import com.app.builder.core.config.ServerFlags
import com.app.builder.core.locale.now
import com.app.builder.core.platform.Env
import com.app.builder.core.platform.Property
import com.app.builder.core.platform.platform
import com.app.builder.core.security.toUuid
import com.app.builder.core.telemetry.SentryLogger
import com.app.builder.core.telemetry.ServerLogger
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.database.DatabaseFactory
import com.app.builder.data.database.DatabaseFactory.getInMemoryConnectionFactory
import com.app.builder.data.database.DatabaseFactory.getPostgresConnectionFactory
import com.app.builder.data.http.URL.Companion.DEV_PORT
import com.app.builder.data.http.installHttpPlugins
import com.app.builder.data.signal.LocalSignal
import com.app.builder.data.signal.PostgresSignal
import com.app.builder.domain.EntityType
import com.app.builder.domain.Permission
import com.app.builder.domain.User
import com.app.builder.domain.RegistrationForm
import com.app.builder.domain.UserCredentials
import com.app.builder.domain.push.FcmManager
import com.app.builder.domain.push.NoOpFcmService
import com.app.builder.domain.route.installRouting
import com.app.builder.test.ExcludeFromTesting
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import io.r2dbc.postgresql.PostgresqlConnectionFactory

/**
 * Entry point of the server application.
 * Starts the server.
 */
@ExcludeFromTesting
fun main() {
    val server = embeddedServer(
        factory = Netty,
        port = Env.port ?: DEV_PORT,
        module = Application::module
    )
    server.addShutdownHook { server.stop() }
    server.start(wait = true)
}

/**
 * Configures the application module.
 * Initializes the database, use case gateways, and installs required server plugins including routing.
 *
 * @receiver [Application] The application instance to configure.
 */
internal suspend fun Application.module() {
    setTelemetry()
    Telemetry.info(tag = TAG, message = "Server started")
    Telemetry.info(tag = TAG, message = "Development mode: ${Env.developmentMode}")
    Telemetry.info(tag = TAG, message = "Platform: $platform")
    Telemetry.info(tag = TAG, message = "Initializing dependencies")
    val dependency = createDependency()
    Telemetry.info(tag = TAG, message = "Installing HTTP plugins")
    installHttpPlugins()
    Telemetry.info(tag = TAG, message = "Setting routes")
    installRouting(dependency = dependency)

    dependency.start()
    seed(dependency = dependency)

    stopOnShutdown(dependency = dependency)
}

/** Stops [Dependency] services once the underlying engine has closed its connectors and routing, as part of its own shutdown. */
private fun Application.stopOnShutdown(dependency: Dependency) {
    monitor.subscribe(definition = ApplicationStopping) {
        Telemetry.info(tag = TAG, message = "Stopping dependencies")
        val stopped = runBlocking { withTimeoutOrNull(timeout = STOP_TIMEOUT) { dependency.stop() } } != null
        if (!stopped) Telemetry.error(tag = TAG, message = "Timed out stopping dependencies after $STOP_TIMEOUT")
    }
}

/** Stops [Dependency] services. */
private suspend fun Dependency.stop() {
    Telemetry.info(tag = TAG, message = "Stopping push service")
    pushService.stop()
    Telemetry.info(tag = TAG, message = "Stopping broadcast service")
    broadcastService.stop()
    Telemetry.info(tag = TAG, message = "Stopping user role cache")
    permissionService.stop()
}


/** Starts [Dependency] services. */
private suspend fun Dependency.start() {
    Telemetry.info(tag = TAG, message = "Loading flags and configs")
    useCases.configUseCases.load()
    Telemetry.info(tag = TAG, message = "Server Feature Flags: ${ServerFlags.flags}")
    Telemetry.info(tag = TAG, message = "Server Configs: ${ServerConfigs.configs}")
    Telemetry.info(tag = TAG, message = "Client Feature Flags: ${ClientFlags.flags}")
    Telemetry.info(tag = TAG, message = "Client Configs: ${ClientConfigs.configs}")

    Telemetry.info(tag = TAG, message = "Starting user role cache")
    permissionService.start()
    Telemetry.info(tag = TAG, message = "Starting broadcast service")
    broadcastService.start()
    Telemetry.info(tag = TAG, message = "Starting push service")
    pushService.start()
}

/** Sets the telemetry engines. */
private fun setTelemetry() {
    Telemetry.engines.apply {
        clear()
        if (Env.developmentMode) {
            add(element = ServerLogger)
        }
        if (!Env.developmentMode && !Env.sentryDsn.isNullOrBlank()) {
            val sentryLogger = SentryLogger.init(
                options = SentryLogger.Options(
                    dsn = Env.sentryDsn,
                    release = "${Property.serverName}@${Property.serverVersion}"
                )
            )
            add(element = sentryLogger)
        }
    }
}

/**
 * Creates the [Dependency] graph.
 *
 * @return The populated [Dependency] instance.
 */
private suspend fun createDependency(): Dependency {
    val connectionFactory = runCatching {
        (if (Env.developmentMode) getInMemoryConnectionFactory() else getPostgresConnectionFactory()).also {
            Telemetry.info(tag = TAG, message = "Connection factory created")
        }
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable to create the connection factory", throwable = it)
    }.getOrThrow()

    val database = runCatching {
        DatabaseFactory.create(connectionFactory = connectionFactory).also {
            Telemetry.info(tag = TAG, message = "Database created")
        }
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable to create the database", throwable = it)
    }.getOrThrow()

    val firebaseApp = runCatching {
        if (!Env.developmentMode && ServerFlags.flags.firebase) {
            FirebaseApp.getApps().firstOrNull() ?: FirebaseApp.initializeApp(
                FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.getApplicationDefault())
                    .setProjectId(Env.gcpProjectId)
                    .build()
            ).also { Telemetry.info(tag = TAG, message = "Firebase app initialized") }
        } else null
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable to init the Firebase app", throwable = it)
    }.getOrNull()

    val fcmService = runCatching {
        if (firebaseApp != null) {
            FcmManager(firebaseMessaging = FirebaseMessaging.getInstance(firebaseApp)).also {
                Telemetry.info(tag = TAG, message = "Firebase Messaging service created")
            }
        } else NoOpFcmService.also {
            Telemetry.info(tag = TAG, message = "NoOp Firebase Messaging service created")
        }
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable to initialize Firebase Manager, FCM push will be a no-op", throwable = it)
    }.getOrDefault(defaultValue = NoOpFcmService)

    val instanceSignal = when (connectionFactory) {
        is PostgresqlConnectionFactory -> PostgresSignal(connectionFactory = connectionFactory)
        else -> LocalSignal()
    }.also { Telemetry.info(tag = TAG, message = "Instance Signal created") }

    return Dependency(
        database = database,
        instanceSignal = instanceSignal,
        fcmService = fcmService,
    ).also { Telemetry.info(tag = TAG, message = "Dependency Index complete") }
}

/**
 * Populates the database.
 *
 * @param dependency The application dependency graph.
 */
private suspend fun seed(dependency: Dependency) {
    val uuid = Env.adminUuid?.toUuid()
    val username = Env.adminUsername
    val password = Env.adminPassword
    if (uuid == null || username.isNullOrBlank() || password.isNullOrBlank()) error(message = "Missing admin configurations")
    val superUserCredentials = UserCredentials(username = username, password = password)
    val superUser = User(
        uuid = uuid,
        modifiedAt = now(),
        deletedAt = null,
        permissions = EntityType.entries.associateWith { Permission.WRITE },
        name = username,
        avatar = null
    )
    val result = dependency.useCases.authenticationUseCases.register(registrationForm = RegistrationForm(user = superUser, credentials = superUserCredentials))
    when (result) {
        null -> Telemetry.info(tag = TAG, message = "Unable to create su account")
        else -> Telemetry.info(tag = TAG, message = "Su account created")
    }
}

private const val TAG = "Server"
private val STOP_TIMEOUT = 15.seconds
