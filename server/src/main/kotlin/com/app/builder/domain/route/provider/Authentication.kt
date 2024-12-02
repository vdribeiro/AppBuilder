package com.app.builder.domain.route.provider

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.http.URL
import com.app.builder.data.serializer.encode
import com.app.builder.domain.EntityType
import com.app.builder.domain.Permission
import com.app.builder.domain.RegistrationForm
import com.app.builder.domain.UserCredentials
import com.app.builder.domain.permission.PermissionService
import com.app.builder.domain.route.receive
import com.app.builder.domain.route.respondSafely
import com.app.builder.domain.route.toRegistry
import com.app.builder.domain.route.validatePermission
import com.app.builder.domain.usecase.authentication.AuthenticationUseCases
import com.app.builder.domain.usecase.registry.RegistryUseCases

/**
 * Configures the authentication routing endpoints for the server.
 * This function sets up the HTTP routes required for user identity management.
 * It delegates the core authentication business logic to the provided [AuthenticationUseCases].
 *
 * @param permissionService The permission service.
 * @param authenticationUseCases The collection of use cases handling authentication logic.
 * @param registryUseCases The registry use cases used to record request telemetry.
 */
fun Route.authenticationRoutes(
    permissionService: PermissionService,
    authenticationUseCases: AuthenticationUseCases,
    registryUseCases: RegistryUseCases
) {
    authenticate {
        post(path = URL.Register.path) {
            if (!call.validatePermission(permissionService = permissionService, entityType = EntityType.SESSION, permission = Permission.WRITE)) return@post
            val registrationForm = call.receive<RegistrationForm>() ?: return@post

            val result = authenticationUseCases.register(registrationForm = registrationForm) ?: run {
                Telemetry.error(tag = TAG, message = "Failed to register user ${registrationForm.user.name}")
                call.respondSafely(status = HttpStatusCode.BadRequest, message = "Invalid registration form")
                return@post
            }

            call.respondSafely(status = HttpStatusCode.OK, message = result)
            Telemetry.info(tag = TAG, message = "Successful registration for user ${registrationForm.user.name}")

            call.toRegistry(
                entityType = EntityType.USER,
                entityUuid = result.user.uuid,
                payload = encode(value = result.user)
            )?.let { registryUseCases.upsertRegistry(registry = it) }
        }
    }

    post(path = URL.Login.path) {
        val credentials = call.receive<UserCredentials>() ?: return@post

        val result = authenticationUseCases.login(credentials = credentials) ?: run {
            Telemetry.error(tag = TAG, message = "Failed login attempt for ${credentials.username}")
            call.respondSafely(status = HttpStatusCode.Unauthorized, message = "Invalid credentials")
            return@post
        }

        call.respondSafely(status = HttpStatusCode.OK, message = result)
        Telemetry.info(tag = TAG, message = "Successful login for user ${credentials.username}")

        call.toRegistry(
            entityType = EntityType.SESSION,
            entityUuid = result.user.uuid,
            userUuid = result.user.uuid,
            payload = "login"
        )?.let { registryUseCases.upsertRegistry(registry = it) }
    }

    post(path = URL.Logout.path) {
        val refreshToken = call.receive<String>() ?: return@post

        val userUuid = authenticationUseCases.logout(refreshToken = refreshToken)
        if (userUuid == null) {
            Telemetry.error(tag = TAG, message = "Failed to logout")
            call.respondSafely(status = HttpStatusCode.Unauthorized, message = "Invalid or expired refresh token")
            return@post
        }

        call.respondSafely(status = HttpStatusCode.OK, message = userUuid)
        Telemetry.info(tag = TAG, message = "Successfully logged out")

        call.toRegistry(
            entityType = EntityType.SESSION,
            entityUuid = userUuid,
            userUuid = userUuid,
            payload = "logout"
        )?.let { registryUseCases.upsertRegistry(registry = it) }
    }

    post(path = URL.RefreshTokens.path) {
        val refreshToken = call.receive<String>() ?: return@post

        val bearerToken = authenticationUseCases.refreshTokens(refreshToken = refreshToken) ?: run {
            Telemetry.error(tag = TAG, message = "Token refresh failed")
            call.respondSafely(status = HttpStatusCode.Unauthorized, message = "Invalid or expired refresh token")
            return@post
        }

        call.respondSafely(status = HttpStatusCode.OK, message = bearerToken)
        Telemetry.info(tag = TAG, message = "Successfully refreshed tokens")
    }
}

private const val TAG = "AuthenticationRoute"