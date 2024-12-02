package com.app.builder.test

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.sse.SSE
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.ApplicationTestBuilder
import com.app.builder.Dependency
import com.app.builder.core.flow.LazyData
import com.app.builder.data.database.DatabaseFactory
import com.app.builder.data.database.create
import com.app.builder.data.database.reset
import com.app.builder.data.http.installHttpPlugins
import com.app.builder.data.signal.LocalSignal
import com.app.builder.domain.RegistrationForm
import com.app.builder.domain.push.NoOpFcmService
import com.app.builder.domain.Authentication
import com.app.builder.domain.BearerToken
import com.app.builder.domain.route.installRouting

/** Abstract class for defining test cases. It provides a hermetic testing environment. */
abstract class TestCase: SharedTestCase() {

    /** Dependency index for test cases with in memory Database and NoOp FCM Service. */
    protected val dependency: LazyData<Dependency> = LazyData {
        Dependency(
            database = DatabaseFactory.create(connectionFactory = DatabaseFactory.getInMemoryConnectionFactory()),
            instanceSignal = LocalSignal(),
            fcmService = NoOpFcmService
        )
    }

    /** Admin bearer token for test cases. */
    protected lateinit var adminBearerToken: BearerToken
    /** User bearer token for test cases. */
    protected lateinit var bearerToken: BearerToken

    override suspend fun beforeTest() {
        dependency.get().database.reset()
        dependency.get().database.create()
        adminBearerToken = seedUser(registrationForm = FakeData.adminRegistrationForm).bearer
        bearerToken = seedUser(registrationForm = FakeData.registrationForm).bearer
    }

    override suspend fun afterTest() {
        dependency.get().database.reset()
    }

    /**
     * Seeds a user.
     *
     * @param registrationForm The registration form to use.
     * @return The [Authentication] of the seeded user.
     */
    protected suspend fun seedUser(registrationForm: RegistrationForm): Authentication = requireNotNull(
        value = dependency.get().useCases.authenticationUseCases.register(registrationForm = registrationForm)
    ) { "Unable to seed the user account." }

    /** Installs HTTP plugins and routing, and starts the broadcast and push service. */
    protected fun ApplicationTestBuilder.installRouting() {
        application {
            val dependency = dependency.get()
            installHttpPlugins()
            installRouting(dependency = dependency)
        }
    }

    /**
     * Creates an HTTP client. Installs the ContentNegotiation, SSE and WebSockets plugins. If the [token] is not null, it also installs the Authentication plugin.
     *
     * @param token The authentication token.
     * @return The created HTTP client.
     */
    protected fun ApplicationTestBuilder.createClient(token: String?): HttpClient = createClient {
        install(plugin = ContentNegotiation) { json() }
        install(plugin = SSE)
        install(plugin = WebSockets)
        token?.let {
            install(plugin = Auth) {
                bearer {
                    loadTokens { BearerTokens(accessToken = it, refreshToken = "") }
                    sendWithoutRequest { true }
                }
            }
        }
    }

    /**
     * Suspends until [condition] holds, failing after [timeout].
     *
     * @param timeout The maximum time to wait.
     * @param condition The condition to await.
     */
    protected suspend fun awaitUntil(timeout: Duration = 5.seconds, condition: suspend () -> Boolean) {
        withTimeout(timeout = timeout) { while (!condition()) delay(timeMillis = 10) }
    }
}
