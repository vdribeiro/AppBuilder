package com.app.builder.domain.route.provider

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import com.app.builder.core.security.toUuid
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.http.URL
import com.app.builder.data.serializer.encode
import com.app.builder.domain.Account
import com.app.builder.domain.EntityType
import com.app.builder.domain.Permission
import com.app.builder.domain.permission.PermissionService
import com.app.builder.domain.permission.UserAccess
import com.app.builder.domain.route.getOrRespond
import com.app.builder.domain.route.getParameters
import com.app.builder.domain.route.receive
import com.app.builder.domain.route.respondSafely
import com.app.builder.domain.route.toRegistry
import com.app.builder.domain.route.validatePermission
import com.app.builder.domain.route.validateTime
import com.app.builder.domain.usecase.account.AccountUseCases
import com.app.builder.domain.usecase.registry.RegistryUseCases

/**
 * Configures the account routes. Accounts are identified by the uuid of the user they belong to, and are created through the registration route.
 *
 * @param permissionService The permission service, updated whenever an account changes.
 * @param accountUseCases The collection of use cases handling account logic.
 * @param registryUseCases The registry use cases used to record request telemetry.
 */
fun Route.accountRoutes(
    permissionService: PermissionService,
    accountUseCases: AccountUseCases,
    registryUseCases: RegistryUseCases
) {
    authenticate {
        route(path = URL.Accounts.path) {
            get(path = "/{uuid}") {
                if (!call.validatePermission(permissionService = permissionService, entityType = EntityType.ACCOUNT, permission = Permission.READ)) return@get

                val uuid = call.getParameters()["uuid"]?.toUuid() ?: run {
                    call.respondSafely(status = HttpStatusCode.BadRequest, message = "Invalid user uuid")
                    return@get
                }

                val account = call.getOrRespond(
                    outcome = accountUseCases.getAccount(userUuid = uuid),
                    notFound = "Account not found",
                    error = "Error getting account"
                ) ?: return@get
                call.respondSafely(status = HttpStatusCode.OK, message = account)
                Telemetry.info(tag = TAG, message = "Fetched the account of user $uuid")

                call.toRegistry(
                    entityType = EntityType.ACCOUNT,
                    entityUuid = uuid
                )?.let { registryUseCases.upsertRegistry(registry = it) }
            }

            post {
                if (!call.validateTime()) return@post
                if (!call.validatePermission(permissionService = permissionService, entityType = EntityType.ACCOUNT, permission = Permission.WRITE)) return@post
                val account = call.receive<Account>() ?: return@post

                call.getOrRespond(
                    outcome = accountUseCases.upsertAccount(account = account),
                    notFound = "Account not found",
                    conflict = "The username is taken",
                    error = "Error upserting account"
                ) ?: return@post
                permissionService.set(userUuid = account.userUuid, access = UserAccess(permissions = account.permissions, deletedAt = account.deletedAt))

                call.respondSafely(status = HttpStatusCode.OK, message = account)
                Telemetry.info(tag = TAG, message = "Upserted the account of user ${account.userUuid}")

                call.toRegistry(
                    entityType = EntityType.ACCOUNT,
                    entityUuid = account.userUuid,
                    payload = encode(value = account)
                )?.let { registryUseCases.upsertRegistry(registry = it) }
            }
        }
    }
}

private const val TAG = "AccountRoute"
