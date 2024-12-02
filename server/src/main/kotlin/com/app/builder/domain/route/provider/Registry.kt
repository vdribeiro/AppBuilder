package com.app.builder.domain.route.provider

import kotlin.time.Instant
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import com.app.builder.core.config.ServerConfigs
import com.app.builder.core.locale.toInstant
import com.app.builder.core.security.toUuid
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.http.Query
import com.app.builder.data.http.URL
import com.app.builder.domain.EntityType
import com.app.builder.domain.Permission
import com.app.builder.domain.Registry
import com.app.builder.domain.permission.PermissionService
import com.app.builder.domain.route.getQueryMap
import com.app.builder.domain.route.getUserUuid
import com.app.builder.domain.route.receive
import com.app.builder.domain.route.respondSafely
import com.app.builder.domain.route.validatePermission
import com.app.builder.domain.route.validateTime
import com.app.builder.domain.usecase.registry.RegistryUseCases

/**
 * Configures the registry routing endpoints for the server.
 * This function sets up the HTTP routes for managing registries.
 * It delegates the core registry business logic to the provided [RegistryUseCases].
 *
 * @param permissionService The permission service.
 * @param registryUseCases The registry use cases used to record request telemetry.
 */
fun Route.registryRoutes(
    permissionService: PermissionService,
    registryUseCases: RegistryUseCases
) {
    authenticate {
        route(path = URL.Registries.path) {
            get {
                if (!call.validateTime()) return@get
                val userUuid = call.getUserUuid() ?: return@get
                if (!call.validatePermission(permissionService = permissionService, entityType = EntityType.REGISTRY, permission = Permission.READ)) return@get

                val queryMap = call.getQueryMap()
                val lastSyncUtc = queryMap[Query.LastSyncUtc.parameter]?.toInstant() ?: Instant.fromEpochSeconds(epochSeconds = 0)
                val cursorUtc = queryMap[Query.CursorUtc.parameter]?.toInstant()
                val cursorUuid = queryMap[Query.CursorUuid.parameter]?.toUuid()
                val pageSize = queryMap[Query.PageSize.parameter]?.toIntOrNull() ?: ServerConfigs.configs.pageSize

                val registries = registryUseCases.getRegistries(lastSyncUtc = lastSyncUtc, cursorUtc = cursorUtc, cursorUuid = cursorUuid, pageSize = pageSize)
                call.respondSafely(status = HttpStatusCode.OK, message = registries)
                Telemetry.info(tag = TAG, message = "Fetched ${registries.size} registries for user $userUuid")
            }

            post {
                if (!call.validateTime()) return@post
                val userUuid = call.getUserUuid() ?: return@post
                if (!call.validatePermission(permissionService = permissionService, entityType = EntityType.REGISTRY, permission = Permission.WRITE)) return@post
                val registry = call.receive<Registry>() ?: return@post

                val success = registryUseCases.upsertRegistry(registry = registry)
                if (!success) {
                    Telemetry.error(tag = TAG, message = "User $userUuid is not authorized to upsert registry ${registry.requestUuid}")
                    call.respondSafely(status = HttpStatusCode.Forbidden, message = "Not authorized to modify this registry")
                    return@post
                }

                call.respondSafely(status = HttpStatusCode.OK, message = registry)
                Telemetry.info(tag = TAG, message = "Upserted registry ${registry.requestUuid} with user $userUuid")
            }
        }
    }
}

private const val TAG = "RegistryRoute"