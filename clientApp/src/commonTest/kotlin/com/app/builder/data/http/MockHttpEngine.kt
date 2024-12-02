package com.app.builder.data.http

import kotlin.uuid.Uuid
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.URLProtocol
import io.ktor.http.content.TextContent
import com.app.builder.core.locale.now
import com.app.builder.data.serializer.decode
import com.app.builder.data.serializer.encode
import com.app.builder.domain.RegistrationForm
import com.app.builder.domain.UserCredentials
import com.app.builder.test.FakeData

/**
 * Creates a platform-specific [HttpClientEngine] instance for test cases.
 *
 * @return An [HttpClientEngine] configured for the current target platform.
 */
fun createMockHttpEngine(): HttpClientEngine = TestEngine(config = MockEngineConfig().apply {
    requestHandlers.add { request ->
        val path = request.url.encodedPath
        when (request.url.protocol) {
            URLProtocol.WS,
            URLProtocol.WSS -> when {
                path.startsWith(prefix = URL.Push.path) -> respondWebSocket(pushPayload = FakeData.notification)
                else -> notFound(request = request)
            }

            URLProtocol.HTTP,
            URLProtocol.HTTPS -> when (request.method) {
                HttpMethod.Get -> when {
                    path.startsWith(prefix = URL.Broadcast.path) -> respondSse(pushPayload = FakeData.notification)
                    path.startsWith(prefix = URL.ClientFlags.path) -> respondMock(content = encode(value = FakeData.clientFlags))
                    path.startsWith(prefix = URL.ClientConfigs.path) -> respondMock(content = encode(value = FakeData.clientConfigs))
                    path.startsWith(prefix = URL.ServerFlags.path) -> respondMock(content = encode(value = FakeData.serverFlags))
                    path.startsWith(prefix = URL.ServerConfigs.path) -> respondMock(content = encode(value = FakeData.serverConfigs))
                    path.startsWith(prefix = URL.Translations.path) -> respondMock(content = encode(value = FakeData.translations))
                    path.startsWith(prefix = "${URL.Users.path}/") -> respondMock(content = encode(value = FakeData.adminUser))
                    path.startsWith(prefix = URL.Users.path) -> respondMock(content = encode(value = listOf(FakeData.adminUser, FakeData.user)))
                    path.startsWith(prefix = "${URL.Tasks.path}/") -> respondMock(content = encode(value = FakeData.task))
                    path.startsWith(prefix = URL.Tasks.path) -> respondMock(content = encode(value = listOf(FakeData.task)))
                    else -> notFound(request = request)
                }

                HttpMethod.Post -> when {
                    path.startsWith(prefix = URL.Tickets.path) -> respondMock(content = Uuid.NIL.toString())
                    path.startsWith(prefix = URL.Register.path) -> {
                        val registrationForm = request.decodeBody<RegistrationForm>() ?: return@add badRequest(request = request)
                        when (registrationForm.user) {
                            FakeData.adminUser -> respondMock(content = encode(value = FakeData.adminAuthentication))
                            FakeData.user -> respondMock(content = encode(value = FakeData.authentication))
                            else -> badRequest(request = request)
                        }
                    }

                    path.startsWith(prefix = URL.Login.path) -> {
                        val credentials = request.decodeBody<UserCredentials>() ?: return@add badRequest(request = request)
                        when (credentials) {
                            FakeData.adminCredentials -> respondMock(content = encode(value = FakeData.adminAuthentication))
                            FakeData.credentials -> respondMock(content = encode(value = FakeData.authentication))
                            else -> badRequest(request = request)
                        }
                    }

                    path.startsWith(prefix = URL.Logout.path) -> respondMock(content = encode(value = "Successful logged out"))
                    path.startsWith(prefix = URL.RefreshTokens.path) -> {
                        val bearerToken = request.headers[HttpHeaders.Authorization]?.removePrefix(prefix = "Bearer ") ?: return@add badRequest(request = request)
                        when (bearerToken) {
                            FakeData.adminBearerToken.accessToken -> respondMock(content = encode(value = FakeData.adminBearerToken))
                            FakeData.bearerToken.accessToken -> respondMock(content = encode(value = FakeData.bearerToken))
                            else -> badRequest(request = request)
                        }
                    }

                    path.startsWith(prefix = URL.ClientFlags.path) -> respondMock(content = encode(value = FakeData.clientFlags))
                    path.startsWith(prefix = URL.ClientConfigs.path) -> respondMock(content = encode(value = FakeData.clientConfigs))
                    path.startsWith(prefix = URL.ServerFlags.path) -> respondMock(content = encode(value = FakeData.serverFlags))
                    path.startsWith(prefix = URL.ServerConfigs.path) -> respondMock(content = encode(value = FakeData.serverConfigs))
                    path.startsWith(prefix = URL.Push.path) -> respondMock(content = encode(value = "sent"))
                    path.startsWith(prefix = URL.Broadcast.path) -> respondMock(content = encode(value = "sent"))
                    path.startsWith(prefix = URL.Tasks.path) -> respondMock(content = encode(value = FakeData.task))
                    path.startsWith(prefix = URL.Users.path) -> respondMock(content = encode(value = FakeData.adminUser))
                    else -> notFound(request = request)
                }

                HttpMethod.Delete -> {
                    val now = now()
                    when {
                        path.startsWith(prefix = URL.Tasks.path) -> respondMock(content = encode(value = FakeData.task.copy(modifiedAt = now, deletedAt = now)))
                        else -> notFound(request = request)
                    }
                }

                else -> badRequest(request = request)
            }

            else -> notFound(request = request)
        }
    }
})

/**
 * Decodes the JSON-serialized request body into [T].
 *
 * @return The decoded body, or null if the body is not JSON text or fails to decode.
 */
private inline fun <reified T> HttpRequestData.decodeBody(): T? = (body as? TextContent)?.text?.let { decode<T>(value = it) }
