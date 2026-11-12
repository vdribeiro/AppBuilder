package com.app.builder.data.http

import kotlin.uuid.Uuid
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.content.TextContent
import com.app.builder.data.serializer.decode
import com.app.builder.data.serializer.encode
import com.app.builder.domain.Account
import com.app.builder.domain.RegistrationForm
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
            HttpMethod.Get -> when {
                path == "${URL.Accounts.path}/${FakeData.adminAccount.userUuid}" -> respondMock(content = encode(value = FakeData.adminAccount))
                path == "${URL.Accounts.path}/${FakeData.account.userUuid}" -> respondMock(content = encode(value = FakeData.account))
                path == URL.Broadcast.path -> respondSse(pushPayload = FakeData.notification)
                path == URL.ClientFlags.path -> respondMock(content = encode(value = FakeData.clientFlags))
                path == URL.ClientConfigs.path -> respondMock(content = encode(value = FakeData.clientConfigs))
                path == URL.ServerFlags.path -> respondMock(content = encode(value = FakeData.serverFlags))
                path == URL.ServerConfigs.path -> respondMock(content = encode(value = FakeData.serverConfigs))
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
                else -> notFound(request = request)
            }

            HttpMethod.Post -> when (path) {
                URL.Tickets.path -> respondMock(content = Uuid.NIL.toString())
                URL.Register.path -> {
                    val registrationForm = request.decodeBody<RegistrationForm>() ?: return@TestEngine badRequest(request = request)
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
                        else -> badRequest(request = request)
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

                URL.ClientFlags.path -> respondMock(content = encode(value = FakeData.clientFlags))
                URL.ClientConfigs.path -> respondMock(content = encode(value = FakeData.clientConfigs))
                URL.ServerFlags.path -> respondMock(content = encode(value = FakeData.serverFlags))
                URL.ServerConfigs.path -> respondMock(content = encode(value = FakeData.serverConfigs))
                URL.Push.path -> respondMock(content = encode(value = "sent"))
                URL.Broadcast.path -> respondMock(content = encode(value = "sent"))
                URL.BroadcastSubscribe.path -> respondMock(content = encode(value = "sent"))
                URL.Tasks.path -> respondMock(content = encode(value = FakeData.task))
                URL.Equipments.path -> respondMock(content = encode(value = FakeData.equipment))
                URL.BuildingEquipments.path -> respondMock(content = encode(value = FakeData.buildingEquipment))
                URL.Buildings.path -> respondMock(content = encode(value = FakeData.building))
                URL.Users.path -> respondMock(content = encode(value = FakeData.adminUser))
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
private fun String.isItemOf(url: URL): Boolean = substringBeforeLast(delimiter = '/') == url.path && Uuid.parseOrNull(uuidString = substringAfterLast(delimiter = '/')) != null
