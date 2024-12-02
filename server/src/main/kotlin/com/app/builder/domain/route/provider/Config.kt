package com.app.builder.domain.route.provider

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import com.app.builder.core.config.ClientConfigs
import com.app.builder.core.config.ClientFlags
import com.app.builder.core.config.ServerConfigs
import com.app.builder.core.config.ServerFlags
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.http.URL
import com.app.builder.data.serializer.encode
import com.app.builder.domain.EntityType
import com.app.builder.domain.Permission
import com.app.builder.domain.permission.PermissionService
import com.app.builder.domain.route.receive
import com.app.builder.domain.route.respondSafely
import com.app.builder.domain.route.toRegistry
import com.app.builder.domain.route.validatePermission
import com.app.builder.domain.usecase.config.ConfigUseCases
import com.app.builder.domain.usecase.registry.RegistryUseCases

/**
 * Configures the config routes.
 *
 * @param permissionService The permission service.
 * @param configUseCases The collection of use cases handling config logic.
 * @param registryUseCases The registry use cases used to record request telemetry.
 */
fun Route.configRoutes(
    permissionService: PermissionService,
    configUseCases: ConfigUseCases,
    registryUseCases: RegistryUseCases
) {
    get(path = URL.ClientFlags.path) {
        val flags = ClientFlags.flags
        call.respondSafely(status = HttpStatusCode.OK, message = flags)
        Telemetry.info(tag = TAG, message = "Fetched client flags")
    }
    get(path = URL.ClientConfigs.path) {
        val configs = ClientConfigs.configs
        call.respondSafely(status = HttpStatusCode.OK, message = configs)
        Telemetry.info(tag = TAG, message = "Fetched client configs")
    }

    authenticate {
        post(path = URL.ClientFlags.path) {
            if (!call.validatePermission(permissionService = permissionService, entityType = EntityType.CLIENT_FLAG, permission = Permission.WRITE)) return@post
            val flags = call.receive<ClientFlags>() ?: return@post
            if (!configUseCases.updateClientFeatureFlags(flags = flags)) {
                call.respondSafely(status = HttpStatusCode.InternalServerError, message = "Unable to update client flags")
                return@post
            }
            call.respondSafely(status = HttpStatusCode.OK, message = ClientFlags.flags)
            Telemetry.info(tag = TAG, message = "Updated client flags")

            call.toRegistry(entityType = EntityType.CLIENT_FLAG, payload = encode(value = ClientFlags.flags))?.let { registryUseCases.upsertRegistry(registry = it) }
        }

        post(path = URL.ClientConfigs.path) {
            if (!call.validatePermission(permissionService = permissionService, entityType = EntityType.CLIENT_CONFIG, permission = Permission.WRITE)) return@post
            val configs = call.receive<ClientConfigs>() ?: return@post
            if (!configUseCases.updateClientConfigs(configs = configs)) {
                call.respondSafely(status = HttpStatusCode.InternalServerError, message = "Unable to update client configs")
                return@post
            }
            call.respondSafely(status = HttpStatusCode.OK, message = ClientConfigs.configs)
            Telemetry.info(tag = TAG, message = "Updated client configs")

            call.toRegistry(entityType = EntityType.CLIENT_CONFIG, payload = encode(value = ClientConfigs.configs))?.let { registryUseCases.upsertRegistry(registry = it) }
        }

        get(path = URL.ServerFlags.path) {
            if (!call.validatePermission(permissionService = permissionService, entityType = EntityType.SERVER_FLAG, permission = Permission.READ)) return@get
            call.respondSafely(status = HttpStatusCode.OK, message = ServerFlags.flags)
            Telemetry.info(tag = TAG, message = "Fetched server flags")
        }

        get(path = URL.ServerConfigs.path) {
            if (!call.validatePermission(permissionService = permissionService, entityType = EntityType.SERVER_CONFIG, permission = Permission.READ)) return@get
            call.respondSafely(status = HttpStatusCode.OK, message = ServerConfigs.configs)
            Telemetry.info(tag = TAG, message = "Fetched server configs")
        }

        post(path = URL.ServerFlags.path) {
            if (!call.validatePermission(permissionService = permissionService, entityType = EntityType.SERVER_FLAG, permission = Permission.WRITE)) return@post
            val flags = call.receive<ServerFlags>() ?: return@post
            if (!configUseCases.updateServerFeatureFlags(flags = flags)) {
                call.respondSafely(status = HttpStatusCode.InternalServerError, message = "Unable to update server flags")
                return@post
            }
            call.respondSafely(status = HttpStatusCode.OK, message = ServerFlags.flags)
            Telemetry.info(tag = TAG, message = "Updated server flags")

            call.toRegistry(entityType = EntityType.SERVER_FLAG, payload = encode(value = ServerFlags.flags))?.let { registryUseCases.upsertRegistry(registry = it) }
        }

        post(path = URL.ServerConfigs.path) {
            if (!call.validatePermission(permissionService = permissionService, entityType = EntityType.SERVER_CONFIG, permission = Permission.WRITE)) return@post
            val configs = call.receive<ServerConfigs>() ?: return@post
            if (!configUseCases.updateServerConfigs(configs = configs)) {
                call.respondSafely(status = HttpStatusCode.InternalServerError, message = "Unable to update server configs")
                return@post
            }
            call.respondSafely(status = HttpStatusCode.OK, message = ServerConfigs.configs)
            Telemetry.info(tag = TAG, message = "Updated server configs")

            call.toRegistry(entityType = EntityType.SERVER_CONFIG, payload = encode(value = ServerConfigs.configs))?.let { registryUseCases.upsertRegistry(registry = it) }
        }
    }
}

private const val TAG = "ConfigRoute"