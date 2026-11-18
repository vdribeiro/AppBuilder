package com.app.builder

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Instant
import kotlin.time.measureTime
import kotlinx.coroutines.flow.firstOrNull
import io.ktor.client.HttpClient
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.server.application.plugin
import io.ktor.server.websocket.WebSockets
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.selectAll
import com.app.builder.core.config.ServerConfigs
import com.app.builder.core.locale.now
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.core.telemetry.TelemetryEngine
import com.app.builder.data.database.safeTransaction
import com.app.builder.data.database.table.UserSessionTable
import com.app.builder.data.http.HttpRequest
import com.app.builder.data.http.URL
import com.app.builder.data.http.post
import com.app.builder.domain.EntityType
import com.app.builder.domain.Permission
import com.app.builder.domain.UserCredentials
import com.app.builder.domain.usecase.Outcome
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase
import io.ktor.client.request.get as ktorGet
import io.ktor.client.request.post as ktorPost

class SecurityTest: TestCase() {

    /** Verifies that refreshing the session of a disabled account does not extend the session's expiry. */
    @Test
    fun disabledAccountSessionStopsSliding() = runServerTest {
        val useCases = dependency.get().useCases

        suspend fun sessionExpiry(): Instant? = dependency.get().database.safeTransaction {
            UserSessionTable.selectAll()
                .where { UserSessionTable.userUuid eq FakeData.user.uuid }
                .firstOrNull()
                ?.get(UserSessionTable.expiresAt)
        }.getOrNull()

        val expiry = sessionExpiry()
        assertEquals(expected = Outcome.Success(data = Unit), actual = useCases.accountUseCases.upsertAccount(account = FakeData.account.copy(deletedAt = now())))

        // The clock is frozen, so a longer validity is what makes an extended expiry observable.
        ServerConfigs.set { it.copy(refreshTokenValidity = it.refreshTokenValidity * 2) }
        useCases.authenticationUseCases.refreshTokens(refreshToken = bearerToken.refreshToken)

        assertEquals(expected = expiry, actual = sessionExpiry())
    }

    /** Verifies that repeated failed logins for one username are throttled well before the global rate limit. */
    @Test
    fun loginThrottlesRepeatedFailures() = runServerTest {
        installRouting()
        val client = createClient(token = null)
        val credentials = UserCredentials(username = FakeData.adminCredentials.username, password = "wrong_password")

        val statuses = List(size = 30) { client.post(request = HttpRequest(url = URL.Login), body = credentials).status }

        assertTrue(actual = HttpStatusCode.TooManyRequests in statuses)
    }

    /** Verifies that rejecting an unknown username takes about as long as rejecting a wrong password. */
    @Test
    fun loginTimingDoesNotRevealUsernames() = runServerTest {
        installRouting()
        val client = createClient(token = null)
        val known = UserCredentials(username = FakeData.adminCredentials.username, password = "wrong_password")
        val unknown = UserCredentials(username = "unknown", password = "wrong_password")

        suspend fun HttpClient.medianLoginTime(credentials: UserCredentials): Duration {
            post(request = HttpRequest(url = URL.Login), body = credentials)
            return List(size = 5) { measureTime { post(request = HttpRequest(url = URL.Login), body = credentials) } }.sorted()[2]
        }

        val knownTime = client.medianLoginTime(credentials = known)
        val unknownTime = client.medianLoginTime(credentials = unknown)

        assertTrue(actual = unknownTime * 2 >= knownTime, message = "Unknown username: $unknownTime, wrong password: $knownTime")
    }

    /** Verifies that registering an account with an empty password is rejected. */
    @Test
    fun registerRejectsEmptyPassword() = runServerTest {
        installRouting()
        val client = createClient(token = adminBearerToken.accessToken)
        val form = registrationForm(username = "empty_password", permissions = emptyMap()).copy(password = "")

        assertEquals(expected = HttpStatusCode.BadRequest, actual = client.post(request = HttpRequest(url = URL.Register), body = form).status)
    }

    /** Verifies that a 16 MiB request body is refused as too large. */
    @Test
    fun oversizedBodyIsRejected() = runServerTest {
        installRouting()
        val client = createClient(token = null)
        val credentials = UserCredentials(username = "unknown", password = "a".repeat(n = 16 * 1024 * 1024))

        assertEquals(expected = HttpStatusCode.PayloadTooLarge, actual = client.post(request = HttpRequest(url = URL.Login), body = credentials).status)
    }

    /** Verifies that the largest WebSocket frame the server accepts is at most 1 MiB. */
    @Test
    fun webSocketFrameSizeIsBounded() = runServerTest {
        installRouting()
        var maxFrameSize = Long.MAX_VALUE
        application { maxFrameSize = plugin(plugin = WebSockets).maxFrameSize }
        startApplication()

        assertTrue(actual = maxFrameSize <= 1024 * 1024, message = "Max frame size: $maxFrameSize")
    }

    /** Verifies that the default page size bound is at most 1000 entries. */
    @Test
    fun pageSizeIsBounded() = runServerTest {
        assertTrue(actual = ServerConfigs.value.maxPageSize <= 1_000, message = "Max page size: ${ServerConfigs.value.maxPageSize}")
    }

    /** Verifies that a refresh token presented again after its grace period ends the session for its current refresh token too. */
    @Test
    fun reusedRefreshTokenRevokesSession() = runServerTest {
        ServerConfigs.set { it.copy(refreshTokenGracePeriod = 0L) }
        val authenticationUseCases = dependency.get().useCases.authenticationUseCases

        val rotated = authenticationUseCases.refreshTokens(refreshToken = bearerToken.refreshToken).successData()
        assertEquals(expected = Outcome.Rejected, actual = authenticationUseCases.refreshTokens(refreshToken = bearerToken.refreshToken))

        assertEquals(expected = Outcome.Rejected, actual = authenticationUseCases.refreshTokens(refreshToken = rotated.refreshToken))
    }

    /** Verifies that a task posted without the registry headers is stored only if a registry records it. */
    @Test
    fun writeWithoutRegistryHeadersIsAudited() = runServerTest {
        installRouting()
        val client = createClient(token = adminBearerToken.accessToken)

        client.post(request = HttpRequest(url = URL.Tasks), body = FakeData.task)

        val stored = client.ktorGet(urlString = "${URL.Tasks.path}/${FakeData.task.uuid}").status == HttpStatusCode.OK
        val registries = dependency.get().useCases.registryUseCases
            .getRegistries(lastSyncUtc = Instant.fromEpochSeconds(epochSeconds = 0), cursorUtc = null, cursorUuid = null, pageSize = 10)
            .successData()
        assertEquals(expected = stored, actual = registries.any { it.entityUuid == FakeData.task.uuid })
    }

    /** Verifies that an access token issued before a permission downgrade stays downgraded after the permission cache is cleared. */
    @Test
    fun downgradedPermissionsSurviveCacheLoss() = runServerTest {
        installRouting()
        val permissionService = dependency.get().permissionService
        val form = registrationForm(username = "task_writer", permissions = mapOf(EntityType.TASK to Permission.WRITE))
        val client = createClient(token = seedUser(registrationForm = form).bearer.accessToken)
        val adminClient = createClient(token = adminBearerToken.accessToken)
        assertEquals(expected = HttpStatusCode.OK, actual = client.post(request = HttpRequest(url = URL.Tasks), body = FakeData.task).status)

        val downgraded = form.account.copy(permissions = mapOf(EntityType.TASK to Permission.READ))
        assertEquals(expected = HttpStatusCode.OK, actual = adminClient.post(request = HttpRequest(url = URL.Accounts), body = downgraded).status)
        assertEquals(expected = HttpStatusCode.Forbidden, actual = client.post(request = HttpRequest(url = URL.Tasks), body = FakeData.task).status)

        permissionService.stop()
        permissionService.start()

        assertEquals(expected = HttpStatusCode.Forbidden, actual = client.post(request = HttpRequest(url = URL.Tasks), body = FakeData.task).status)
    }

    /** Verifies that a malformed login body is rejected without its password reaching the telemetry. */
    @Test
    fun malformedPayloadIsNotLogged() = runServerTest {
        installRouting()
        val logged = mutableListOf<String>()
        Telemetry.engines += object: TelemetryEngine {
            override fun info(tag: String, message: String) {
                logged += message
            }

            override fun error(tag: String, message: String, throwable: Throwable?) {
                logged += message
                logged += generateSequence(seed = throwable) { it.cause }.map { it.message.orEmpty() }
            }

            override fun feedback(message: String) {
                logged += message
            }
        }
        val password = "Sup3rSecretMarker"
        val body = """{"username":"admin","password":"$password",}"""

        val response = createClient(token = null).ktorPost(urlString = URL.Login.path) {
            setBody(body = TextContent(text = body, contentType = ContentType.Application.Json))
        }

        assertEquals(expected = HttpStatusCode.BadRequest, actual = response.status)
        assertFalse(actual = logged.any { it.contains(other = password) }, message = logged.filter { it.contains(other = password) }.joinToString(separator = "\n"))
    }
}
