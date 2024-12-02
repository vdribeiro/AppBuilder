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
import com.app.builder.data.serializer.encode
import com.app.builder.domain.EntityType
import com.app.builder.domain.Permission
import com.app.builder.domain.Task
import com.app.builder.domain.permission.PermissionService
import com.app.builder.domain.route.getParameters
import com.app.builder.domain.route.getQueryMap
import com.app.builder.domain.route.getUserUuid
import com.app.builder.domain.route.receive
import com.app.builder.domain.route.respondSafely
import com.app.builder.domain.route.toRegistry
import com.app.builder.domain.route.validatePermission
import com.app.builder.domain.route.validateTime
import com.app.builder.domain.usecase.registry.RegistryUseCases
import com.app.builder.domain.usecase.task.TaskUseCases

/**
 * Configures the task routing endpoints for the server.
 * This function sets up the HTTP routes for managing tasks.
 * It delegates the core task business logic to the provided [TaskUseCases].
 *
 * @param permissionService The permission service.
 * @param taskUseCases The collection of use cases handling task logic.
 * @param registryUseCases The registry use cases used to record request telemetry.
 */
fun Route.taskRoutes(
    permissionService: PermissionService,
    taskUseCases: TaskUseCases,
    registryUseCases: RegistryUseCases
) {
    authenticate {
        route(path = URL.Tasks.path) {
            get {
                if (!call.validateTime()) return@get
                val userUuid = call.getUserUuid() ?: return@get
                if (!call.validatePermission(permissionService = permissionService, entityType = EntityType.TASK, permission = Permission.READ)) return@get

                val queryMap = call.getQueryMap()
                val lastSyncUtc = queryMap[Query.LastSyncUtc.parameter]?.toInstant() ?: Instant.fromEpochSeconds(epochSeconds = 0)
                val cursorUtc = queryMap[Query.CursorUtc.parameter]?.toInstant()
                val cursorUuid = queryMap[Query.CursorUuid.parameter]?.toUuid()
                val pageSize = queryMap[Query.PageSize.parameter]?.toIntOrNull() ?: ServerConfigs.configs.pageSize

                val tasks = taskUseCases.getTasks(userUuid = userUuid, lastSyncUtc = lastSyncUtc, cursorUtc = cursorUtc, cursorUuid = cursorUuid, pageSize = pageSize)
                call.respondSafely(status = HttpStatusCode.OK, message = tasks)
                Telemetry.info(tag = TAG, message = "Fetched ${tasks.size} tasks for user $userUuid")

                call.toRegistry(entityType = EntityType.TASK)?.let { registryUseCases.upsertRegistry(registry = it) }
            }

            get(path = "/{uuid}") {
                if (!call.validateTime()) return@get
                val userUuid = call.getUserUuid() ?: return@get
                if (!call.validatePermission(permissionService = permissionService, entityType = EntityType.TASK, permission = Permission.READ)) return@get

                val uuid = call.getParameters()["uuid"]?.toUuid() ?: run {
                    call.respondSafely(status = HttpStatusCode.BadRequest, message = "Invalid task uuid")
                    return@get
                }

                val task = taskUseCases.getTask(userUuid = userUuid, uuid = uuid) ?: run {
                    call.respondSafely(status = HttpStatusCode.NotFound, message = "Task not found")
                    return@get
                }
                call.respondSafely(status = HttpStatusCode.OK, message = task)
                Telemetry.info(tag = TAG, message = "Fetched task $uuid for user $userUuid")

                call.toRegistry(
                    entityType = EntityType.TASK,
                    entityUuid = uuid
                )?.let { registryUseCases.upsertRegistry(registry = it) }
            }

            post {
                if (!call.validateTime()) return@post
                val userUuid = call.getUserUuid() ?: return@post
                if (!call.validatePermission(permissionService = permissionService, entityType = EntityType.TASK, permission = Permission.WRITE)) return@post
                val task = call.receive<Task>() ?: return@post

                val success = taskUseCases.upsertTask(userUuid = userUuid, task = task)
                if (!success) {
                    Telemetry.error(tag = TAG, message = "User $userUuid is not authorized to upsert task ${task.uuid}")
                    call.respondSafely(status = HttpStatusCode.Forbidden, message = "Not authorized to modify this task")
                    return@post
                }

                call.respondSafely(status = HttpStatusCode.OK, message = task)
                Telemetry.info(tag = TAG, message = "Upserted task ${task.uuid} for user $userUuid")

                call.toRegistry(
                    entityType = EntityType.TASK,
                    entityUuid = task.uuid,
                    payload = encode(value = task)
                )?.let { registryUseCases.upsertRegistry(registry = it) }
            }
        }
    }
}

private const val TAG = "TaskRoute"