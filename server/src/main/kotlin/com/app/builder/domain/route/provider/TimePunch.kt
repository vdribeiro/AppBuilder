package com.app.builder.domain.route.provider

import kotlin.time.Instant
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import com.app.builder.core.config.ServerConfigs
import com.app.builder.core.locale.UNIX_EPOCH
import com.app.builder.core.locale.toInstant
import com.app.builder.core.security.toUuid
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.http.Query
import com.app.builder.data.http.URL
import com.app.builder.data.serializer.encode
import com.app.builder.domain.EntityType
import com.app.builder.domain.Permission
import com.app.builder.domain.TimePunch
import com.app.builder.domain.grants
import com.app.builder.domain.permission.PermissionService
import com.app.builder.domain.route.getAccess
import com.app.builder.domain.route.getOrRespond
import com.app.builder.domain.route.getParameters
import com.app.builder.domain.route.getQueryMap
import com.app.builder.domain.route.getUserUuid
import com.app.builder.domain.route.receive
import com.app.builder.domain.route.respondSafely
import com.app.builder.domain.route.toRegistry
import com.app.builder.domain.route.validateActiveOrDraining
import com.app.builder.domain.route.validatePermission
import com.app.builder.domain.route.validateTime
import com.app.builder.domain.usecase.Outcome
import com.app.builder.domain.usecase.registry.RegistryUseCases
import com.app.builder.domain.usecase.timepunch.TimePunchUseCases

/**
 * Configures the time punch routing endpoints for the server.
 * This function sets up the HTTP routes for managing time punches.
 * It delegates the core time punch business logic to the provided [TimePunchUseCases].
 *
 * @param permissionService The permission service.
 * @param timePunchUseCases The collection of use cases handling time punch logic.
 * @param registryUseCases The registry use cases used to record request telemetry.
 */
fun Route.timePunchRoutes(
    permissionService: PermissionService,
    timePunchUseCases: TimePunchUseCases,
    registryUseCases: RegistryUseCases
) {
    authenticate {
        route(path = URL.TimePunches.path) {
            get {
                if (!call.validateTime()) return@get
                val userUuid = call.getUserUuid() ?: return@get
                val access = call.getAccess(permissionService = permissionService) ?: return@get
                if (!call.validateActiveOrDraining(permissionService = permissionService)) return@get

                // A user with read permission gets the time punches of every user, while any other user only gets its own
                val readable = access.permissions.grants(entityType = EntityType.TIME_PUNCH, permission = Permission.READ)

                val queryMap = call.getQueryMap()
                val lastSyncUtc = queryMap[Query.LastSyncUtc.parameter]?.toInstant() ?: Instant.UNIX_EPOCH
                val cursorUtc = queryMap[Query.CursorUtc.parameter]?.toInstant()
                val cursorUuid = queryMap[Query.CursorUuid.parameter]?.toUuid()
                val pageSize = queryMap[Query.PageSize.parameter]?.toIntOrNull() ?: ServerConfigs.value.pageSize

                val timePunches = call.getOrRespond(
                    outcome = timePunchUseCases.getTimePunches(
                        userUuid = userUuid.takeUnless { readable },
                        lastSyncUtc = lastSyncUtc,
                        cursorUtc = cursorUtc,
                        cursorUuid = cursorUuid,
                        pageSize = pageSize
                    ),
                    error = "Error getting time punches"
                ) ?: return@get
                call.respondSafely(status = HttpStatusCode.OK, message = timePunches)
                Telemetry.info(tag = TAG, message = "Fetched ${timePunches.size} time punches")
            }

            get(path = "/{uuid}") {
                if (!call.validateTime()) return@get
                val userUuid = call.getUserUuid() ?: return@get

                val uuid = call.getParameters()["uuid"]?.toUuid() ?: run {
                    call.respondSafely(status = HttpStatusCode.BadRequest, message = "Invalid time punch uuid")
                    return@get
                }

                val timePunch = call.getOrRespond(
                    outcome = timePunchUseCases.getTimePunch(uuid = uuid),
                    notFound = "Time punch not found",
                    error = "Error getting time punch"
                ) ?: return@get

                // A user with read permission can get any time punch, while any other user can only get its own
                val authorized = when (timePunch.userUuid) {
                    userUuid -> call.validateActiveOrDraining(permissionService = permissionService)
                    else -> call.validatePermission(permissionService = permissionService, entityType = EntityType.TIME_PUNCH, permission = Permission.READ)
                }
                if (!authorized) return@get

                call.respondSafely(status = HttpStatusCode.OK, message = timePunch)
                Telemetry.info(tag = TAG, message = "Fetched time punch $uuid")
            }

            post {
                if (!call.validateTime()) return@post
                val userUuid = call.getUserUuid() ?: return@post
                val timePunch = call.receive<TimePunch>() ?: return@post

                val dbTimePunch = when (val outcome = timePunchUseCases.getTimePunch(uuid = timePunch.uuid)) {
                    is Outcome.Success -> outcome.data
                    Outcome.NotFound -> null
                    is Outcome.Failure -> {
                        call.getOrRespond(outcome = outcome, error = "Error getting time punch ${timePunch.uuid}")
                        return@post
                    }
                }
                // A user with write permission can change any time punch, while any other user can only create its own, or send it again unchanged
                val created = timePunch.userUuid == userUuid && timePunch.deletedAt == null && (dbTimePunch == null || dbTimePunch.matches(other = timePunch))
                val authorized = when {
                    created -> call.validateActiveOrDraining(permissionService = permissionService)
                    else -> call.validatePermission(permissionService = permissionService, entityType = EntityType.TIME_PUNCH, permission = Permission.WRITE)
                }
                if (!authorized) return@post

                call.getOrRespond(outcome = timePunchUseCases.upsertTimePunch(timePunch = timePunch), error = "Error upserting time punch") ?: return@post

                call.respondSafely(status = HttpStatusCode.OK, message = timePunch)
                Telemetry.info(tag = TAG, message = "Upserted time punch ${timePunch.uuid} for user $userUuid")

                call.toRegistry(
                    entityType = EntityType.TIME_PUNCH,
                    entityUuid = timePunch.uuid,
                    payload = encode(value = timePunch)
                )?.let { registryUseCases.upsertRegistry(registry = it) }
            }
        }
    }
}

/**
 * Checks if this time punch holds the same values as [other], comparing its timestamps to the millisecond.
 *
 * @receiver The time punch to compare.
 * @param other The time punch to compare with.
 * @return `true` if both hold the same values, `false` otherwise.
 */
private fun TimePunch.matches(other: TimePunch): Boolean =
    uuid == other.uuid &&
        userUuid == other.userUuid &&
        source == other.source &&
        time.toEpochMilliseconds() == other.time.toEpochMilliseconds() &&
        modifiedAt.toEpochMilliseconds() == other.modifiedAt.toEpochMilliseconds() &&
        deletedAt?.toEpochMilliseconds() == other.deletedAt?.toEpochMilliseconds()

private const val TAG = "TimePunchRoute"
