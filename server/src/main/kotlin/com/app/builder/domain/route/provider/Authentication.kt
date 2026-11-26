package com.app.builder.domain.route.provider

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.http.URL
import com.app.builder.data.serializer.encode
import com.app.builder.domain.EntityType
import com.app.builder.domain.PasswordChange
import com.app.builder.domain.Permission
import com.app.builder.domain.RegistrationForm
import com.app.builder.domain.UserCredentials
import com.app.builder.domain.permission.PermissionService
import com.app.builder.domain.route.getOrRespond
import com.app.builder.domain.route.receive
import com.app.builder.domain.route.respondFailure
import com.app.builder.domain.route.respondSafely
import com.app.builder.domain.route.toRegistry
import com.app.builder.domain.route.validatePermission
import com.app.builder.domain.usecase.authentication.AuthenticationUseCases
import com.app.builder.domain.usecase.authentication.LoginOutcome
import com.app.builder.domain.usecase.authentication.PasswordChangeOutcome
import com.app.builder.domain.usecase.authentication.RegisterOutcome
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
            if (!call.validatePermission(permissionService = permissionService, entityType = EntityType.ACCOUNT, permission = Permission.WRITE)) return@post
            val registrationForm = call.receive<RegistrationForm>() ?: return@post
            if (registrationForm.account.userUuid != registrationForm.user.uuid) {
                call.respondSafely(status = HttpStatusCode.BadRequest, message = "Invalid registration form")
                return@post
            }

            when (val outcome = authenticationUseCases.register(registrationForm = registrationForm)) {
                RegisterOutcome.Success -> Unit
                RegisterOutcome.Invalid -> return@post call.respondFailure(status = HttpStatusCode.BadRequest, message = "Invalid registration form")
                is RegisterOutcome.InvalidPassword -> return@post call.respondFailure(status = HttpStatusCode.BadRequest, message = outcome.failure)
                RegisterOutcome.Conflict -> return@post call.respondFailure(status = HttpStatusCode.Conflict, message = "The user already has an account or the username is taken")
                RegisterOutcome.Error -> return@post call.respondFailure(status = HttpStatusCode.InternalServerError, message = "Error registering account")
            }
            val account = registrationForm.account

            call.respondSafely(status = HttpStatusCode.OK, message = account)
            Telemetry.info(tag = TAG, message = "Successful registration for user ${account.userUuid}")

            call.toRegistry(
                entityType = EntityType.ACCOUNT,
                entityUuid = account.userUuid,
                payload = encode(value = account)
            )?.let { registryUseCases.upsertRegistry(registry = it) }
        }
    }

    post(path = URL.Login.path) {
        val credentials = call.receive<UserCredentials>() ?: return@post

        val result = when (val outcome = authenticationUseCases.login(credentials = credentials)) {
            is LoginOutcome.Success -> outcome.authentication
            LoginOutcome.Rejected -> return@post call.respondFailure(status = HttpStatusCode.Unauthorized, message = "Invalid credentials")
            LoginOutcome.Disabled -> return@post call.respondFailure(status = HttpStatusCode.Forbidden, message = "Account disabled")
            LoginOutcome.Locked -> return@post call.respondFailure(status = HttpStatusCode.TooManyRequests, message = "Too many failed logins")
            LoginOutcome.Error -> return@post call.respondFailure(status = HttpStatusCode.InternalServerError, message = "Unable to login")
        }

        call.respondSafely(status = HttpStatusCode.OK, message = result)
        Telemetry.info(tag = TAG, message = "Successful login for user ${result.user.uuid}")

        call.toRegistry(
            entityType = EntityType.ACCOUNT,
            entityUuid = result.user.uuid,
            userUuid = result.user.uuid,
            payload = "login"
        )?.let { registryUseCases.upsertRegistry(registry = it) }
    }

    post(path = URL.Logout.path) {
        val refreshToken = call.receive<String>() ?: return@post

        val userUuid = call.getOrRespond(
            outcome = authenticationUseCases.logout(refreshToken = refreshToken),
            rejected = "Invalid or expired refresh token",
            error = "Unable to logout"
        ) ?: return@post

        call.respondSafely(status = HttpStatusCode.OK, message = userUuid)
        Telemetry.info(tag = TAG, message = "Successfully logged out")

        call.toRegistry(
            entityType = EntityType.ACCOUNT,
            entityUuid = userUuid,
            userUuid = userUuid,
            payload = "logout"
        )?.let { registryUseCases.upsertRegistry(registry = it) }
    }

    post(path = URL.RefreshTokens.path) {
        val refreshToken = call.receive<String>() ?: return@post

        val bearerToken = call.getOrRespond(
            outcome = authenticationUseCases.refreshTokens(refreshToken = refreshToken),
            rejected = "Invalid or expired refresh token",
            error = "Unable to refresh tokens"
        ) ?: return@post

        call.respondSafely(status = HttpStatusCode.OK, message = bearerToken)
        Telemetry.info(tag = TAG, message = "Successfully refreshed tokens")
    }

    post(path = URL.ChangePassword.path) {
        val passwordChange = call.receive<PasswordChange>() ?: return@post

        val userUuid = when (val outcome = authenticationUseCases.changePassword(passwordChange = passwordChange)) {
            is PasswordChangeOutcome.Success -> outcome.userUuid
            PasswordChangeOutcome.Rejected -> return@post call.respondFailure(status = HttpStatusCode.Unauthorized, message = "Invalid or expired refresh token")
            is PasswordChangeOutcome.InvalidPassword -> return@post call.respondFailure(status = HttpStatusCode.BadRequest, message = outcome.failure)
            PasswordChangeOutcome.Error -> return@post call.respondFailure(status = HttpStatusCode.InternalServerError, message = "Unable to change password")
        }

        call.respondSafely(status = HttpStatusCode.OK, message = userUuid)
        Telemetry.info(tag = TAG, message = "Successfully changed the password of user $userUuid")

        call.toRegistry(
            entityType = EntityType.ACCOUNT,
            entityUuid = userUuid,
            userUuid = userUuid,
            payload = "change_password"
        )?.let { registryUseCases.upsertRegistry(registry = it) }
    }
}

private const val TAG = "AuthenticationRoute"