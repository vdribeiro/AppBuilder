package com.app.builder.data.http

import kotlin.uuid.Uuid
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.content.TextContent
import com.app.builder.core.config.ClientConfigs
import com.app.builder.core.config.ClientFlags
import com.app.builder.core.config.ServerConfigs
import com.app.builder.core.config.ServerFlags
import com.app.builder.core.security.toUuid
import com.app.builder.data.serializer.decode
import com.app.builder.data.serializer.encode
import com.app.builder.domain.Account
import com.app.builder.domain.Building
import com.app.builder.domain.BuildingEquipment
import com.app.builder.domain.BuildingMaterial
import com.app.builder.domain.BuildingUser
import com.app.builder.domain.Equipment
import com.app.builder.domain.EquipmentUser
import com.app.builder.domain.Material
import com.app.builder.domain.PasswordChange
import com.app.builder.domain.PasswordFailure
import com.app.builder.domain.RegistrationForm
import com.app.builder.domain.Task
import com.app.builder.domain.Translation
import com.app.builder.domain.User
import com.app.builder.domain.UserCredentials
import com.app.builder.test.FakeData

/**
 * Creates a platform-specific [HttpClientEngine] instance for test cases.
 *
 * @return An [HttpClientEngine] configured for the current target platform.
 */
fun createMockHttpEngine(): HttpClientEngine = TestEngine { request ->
    val path = request.url.encodedPath
    when (request.url.protocol) {
        URLProtocol.WS,
        URLProtocol.WSS -> when {
            path == URL.Push.path -> respondWebSocket(pushPayload = FakeData.notification)
            else -> notFound(request = request)
        }

        URLProtocol.HTTP,
        URLProtocol.HTTPS -> when (request.method) {
            HttpMethod.Head -> when (path) {
                URL.Probe.path -> respondMock()
                else -> notFound(request = request)
            }

            HttpMethod.Get -> when {
                path == "${URL.Accounts.path}/${FakeData.adminAccount.userUuid}" -> respondMock(content = encode(value = FakeData.adminAccount))
                path == "${URL.Accounts.path}/${FakeData.account.userUuid}" -> respondMock(content = encode(value = FakeData.account))
                path == URL.Broadcast.path -> respondSse(pushPayload = FakeData.notification)
                path == URL.ClientFlags.path -> respondMock(content = encode(value = ClientFlags.value))
                path == URL.ClientConfigs.path -> respondMock(content = encode(value = ClientConfigs.value))
                path == URL.ServerFlags.path -> respondMock(content = encode(value = ServerFlags.value))
                path == URL.ServerConfigs.path -> respondMock(content = encode(value = ServerConfigs.value))
                path == URL.Translations.path -> respondMock(content = encode(value = FakeData.translations))
                path == URL.Registries.path -> respondMock(content = encode(value = listOf(FakeData.registry)))
                path.isItemOf(url = URL.Users) -> respondMock(content = encode(value = FakeData.adminUser))
                path == URL.Users.path -> respondMock(content = encode(value = listOf(FakeData.adminUser, FakeData.user)))
                path.isItemOf(url = URL.Tasks) -> respondMock(content = encode(value = FakeData.task))
                path == URL.Tasks.path -> respondMock(content = encode(value = listOf(FakeData.task)))
                path.isItemOf(url = URL.Buildings) -> respondMock(content = encode(value = FakeData.building))
                path == URL.Buildings.path -> respondMock(content = encode(value = listOf(FakeData.building)))
                path.isItemOf(url = URL.Equipments) -> respondMock(content = encode(value = FakeData.equipment))
                path == URL.Equipments.path -> respondMock(content = encode(value = listOf(FakeData.equipment)))
                path.isItemOf(url = URL.BuildingEquipments) -> respondMock(content = encode(value = FakeData.buildingEquipment))
                path == URL.BuildingEquipments.path -> respondMock(content = encode(value = listOf(FakeData.buildingEquipment)))
                path.isItemOf(url = URL.Materials) -> respondMock(content = encode(value = FakeData.material))
                path == URL.Materials.path -> respondMock(content = encode(value = listOf(FakeData.material)))
                path.isItemOf(url = URL.BuildingMaterials) -> respondMock(content = encode(value = FakeData.buildingMaterial))
                path == URL.BuildingMaterials.path -> respondMock(content = encode(value = listOf(FakeData.buildingMaterial)))
                path.isItemOf(url = URL.BuildingUsers) -> respondMock(content = encode(value = FakeData.buildingUser))
                path == URL.BuildingUsers.path -> respondMock(content = encode(value = listOf(FakeData.buildingUser)))
                path.isItemOf(url = URL.EquipmentUsers) -> respondMock(content = encode(value = FakeData.equipmentUser))
                path == URL.EquipmentUsers.path -> respondMock(content = encode(value = listOf(FakeData.equipmentUser)))
                else -> notFound(request = request)
            }

            HttpMethod.Post -> when (path) {
                URL.Tickets.path -> respondMock(content = Uuid.NIL.toString())
                URL.Register.path -> {
                    val registrationForm = request.decodeBody<RegistrationForm>() ?: return@TestEngine badRequest(request = request)
                    if (registrationForm.password.length < MIN_PASSWORD_LENGTH) return@TestEngine passwordRefused(failure = PasswordFailure.TooShort(minLength = MIN_PASSWORD_LENGTH))
                    respondMock(content = encode(value = registrationForm.account))
                }

                URL.Accounts.path -> {
                    val account = request.decodeBody<Account>() ?: return@TestEngine badRequest(request = request)
                    respondMock(content = encode(value = account))
                }

                URL.Login.path -> {
                    val credentials = request.decodeBody<UserCredentials>() ?: return@TestEngine badRequest(request = request)
                    when (credentials) {
                        FakeData.adminCredentials -> respondMock(content = encode(value = FakeData.adminAuthentication))
                        FakeData.credentials -> respondMock(content = encode(value = FakeData.authentication))
                        else -> when (credentials.username) {
                            DISABLED_USERNAME -> respondMock(status = HttpStatusCode.Forbidden)
                            RATE_LIMITED_USERNAME -> respondMock(status = HttpStatusCode.TooManyRequests)
                            else -> respondMock(status = HttpStatusCode.Unauthorized)
                        }
                    }
                }

                URL.Logout.path -> respondMock(content = encode(value = "Successful logged out"))
                URL.RefreshTokens.path -> {
                    val refreshToken = request.decodeBody<String>() ?: return@TestEngine badRequest(request = request)
                    when (refreshToken) {
                        FakeData.adminBearerToken.refreshToken -> respondMock(content = encode(value = FakeData.adminBearerToken))
                        FakeData.bearerToken.refreshToken -> respondMock(content = encode(value = FakeData.bearerToken))
                        else -> respondMock(status = HttpStatusCode.Unauthorized, content = encode(value = "Invalid or expired refresh token"))
                    }
                }

                URL.ChangePassword.path -> {
                    val passwordChange = request.decodeBody<PasswordChange>() ?: return@TestEngine badRequest(request = request)
                    if (passwordChange.newPassword.length < MIN_PASSWORD_LENGTH) return@TestEngine passwordRefused(failure = PasswordFailure.TooShort(minLength = MIN_PASSWORD_LENGTH))
                    when (passwordChange.currentPassword) {
                        WRONG_PASSWORD -> return@TestEngine passwordRefused(failure = PasswordFailure.WrongPassword(remainingAttempts = 2))
                        LAST_WRONG_PASSWORD -> return@TestEngine respondMock(status = HttpStatusCode.Unauthorized)
                    }
                    when (passwordChange.refreshToken to passwordChange.currentPassword) {
                        FakeData.adminBearerToken.refreshToken to FakeData.adminCredentials.password -> respondMock(content = encode(value = FakeData.adminUser.uuid.toString()))
                        FakeData.bearerToken.refreshToken to FakeData.credentials.password -> respondMock(content = encode(value = FakeData.user.uuid.toString()))
                        else -> badRequest(request = request)
                    }
                }

                URL.ClientFlags.path -> {
                    val clientFlags = request.decodeBody<ClientFlags>() ?: return@TestEngine badRequest(request = request)
                    respondMock(content = encode(value = clientFlags))
                }

                URL.ClientConfigs.path -> {
                    val clientConfigs = request.decodeBody<ClientConfigs>() ?: return@TestEngine badRequest(request = request)
                    respondMock(content = encode(value = clientConfigs))
                }

                URL.ServerFlags.path -> {
                    val serverFlags = request.decodeBody<ServerFlags>() ?: return@TestEngine badRequest(request = request)
                    respondMock(content = encode(value = serverFlags))
                }

                URL.ServerConfigs.path -> {
                    val serverConfigs = request.decodeBody<ServerConfigs>() ?: return@TestEngine badRequest(request = request)
                    respondMock(content = encode(value = serverConfigs))
                }

                URL.Translations.path -> {
                    val translation = request.decodeBody<Translation>() ?: return@TestEngine badRequest(request = request)
                    respondMock(content = encode(value = translation))
                }

                URL.Push.path -> respondMock(content = encode(value = "sent"))
                URL.Broadcast.path -> respondMock(content = encode(value = "sent"))
                URL.BroadcastSubscribe.path -> respondMock(content = encode(value = "sent"))
                URL.Tasks.path -> {
                    val task = request.decodeBody<Task>() ?: return@TestEngine badRequest(request = request)
                    respondMock(content = encode(value = task))
                }

                URL.Equipments.path -> {
                    val equipment = request.decodeBody<Equipment>() ?: return@TestEngine badRequest(request = request)
                    respondMock(content = encode(value = equipment))
                }

                URL.BuildingEquipments.path -> {
                    val buildingEquipment = request.decodeBody<BuildingEquipment>() ?: return@TestEngine badRequest(request = request)
                    respondMock(content = encode(value = buildingEquipment))
                }

                URL.Materials.path -> {
                    val material = request.decodeBody<Material>() ?: return@TestEngine badRequest(request = request)
                    respondMock(content = encode(value = material))
                }

                URL.BuildingMaterials.path -> {
                    val buildingMaterial = request.decodeBody<BuildingMaterial>() ?: return@TestEngine badRequest(request = request)
                    respondMock(content = encode(value = buildingMaterial))
                }

                URL.BuildingUsers.path -> {
                    val buildingUser = request.decodeBody<BuildingUser>() ?: return@TestEngine badRequest(request = request)
                    respondMock(content = encode(value = buildingUser))
                }

                URL.EquipmentUsers.path -> {
                    val equipmentUser = request.decodeBody<EquipmentUser>() ?: return@TestEngine badRequest(request = request)
                    respondMock(content = encode(value = equipmentUser))
                }

                URL.Buildings.path -> {
                    val building = request.decodeBody<Building>() ?: return@TestEngine badRequest(request = request)
                    respondMock(content = encode(value = building))
                }

                URL.Users.path -> {
                    val user = request.decodeBody<User>() ?: return@TestEngine badRequest(request = request)
                    respondMock(content = encode(value = user))
                }

                else -> notFound(request = request)
            }

            else -> badRequest(request = request)
        }

        else -> notFound(request = request)
    }
}

/**
 * Decodes the JSON-serialized request body into [T].
 *
 * @return The decoded body, or null if the body is not JSON text or fails to decode.
 */
private inline fun <reified T> HttpRequestData.decodeBody(): T? = (body as? TextContent)?.text?.let { decode<T>(value = it) }

/**
 * Checks whether this path is [url]'s path followed by a single UUID segment.
 *
 * @param url The collection URL the item belongs to.
 * @return `true` if this path addresses an item of [url].
 */
private fun String.isItemOf(url: URL): Boolean = substringBeforeLast(delimiter = '/') == url.path && substringAfterLast(delimiter = '/').toUuid() != null

/**
 * Responds 400 Bad Request with the reason a password was refused.
 *
 * @param failure Why the password was refused.
 * @return [HttpResponseData] to send.
 */
private fun MockRequestHandleScope.passwordRefused(failure: PasswordFailure): HttpResponseData =
    respondMock(status = HttpStatusCode.BadRequest, content = encode(value = failure))

/** The minimum password length the mock server accepts. */
private const val MIN_PASSWORD_LENGTH = 10

/** A current password the mock server refuses as wrong, with attempts left. */
private const val WRONG_PASSWORD = "wrong_password"

/** A current password the mock server answers by ending the session. */
private const val LAST_WRONG_PASSWORD = "last_wrong_password"

/** A username the mock server answers as a disabled account. */
private const val DISABLED_USERNAME = "disabled_user"

/** A username the mock server answers as rate limited. */
private const val RATE_LIMITED_USERNAME = "rate_limited_user"
