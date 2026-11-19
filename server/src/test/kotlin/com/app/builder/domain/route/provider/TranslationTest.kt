package com.app.builder.domain.route.provider

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import io.ktor.client.call.body
import io.ktor.http.HttpStatusCode
import com.app.builder.core.config.ServerFlags
import com.app.builder.core.locale.now
import com.app.builder.data.database.reset
import com.app.builder.data.http.HttpRequest
import com.app.builder.data.http.URL
import com.app.builder.data.http.get
import com.app.builder.data.http.post
import com.app.builder.data.http.sse
import com.app.builder.data.serializer.decode
import com.app.builder.domain.PushPayload
import com.app.builder.domain.Translation
import com.app.builder.domain.usecase.Outcome
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class TranslationTest: TestCase() {

    override suspend fun beforeTest() {
        super.beforeTest()
        ServerFlags.set { it.copy(translations = true) }
    }

    /** Verifies that translations can be fetched without authentication. */
    @Test
    fun getTranslations() = runServerTest {
        installRouting()
        val client = createClient(token = null)
        assertEquals(expected = Outcome.Success(data = Unit), actual = dependency.get().useCases.translationUseCases.seedTranslations(translations = FakeData.translations))

        val response = client.get(request = HttpRequest(url = URL.Translations))

        assertEquals(expected = HttpStatusCode.OK, actual = response.status)
        assertEquals(expected = FakeData.translations.toSet(), actual = response.body<List<Translation>>().toSet())
    }

    /** Verifies that the translations endpoint responds not found when disabled by flag. */
    @Test
    fun getTranslationsIsDisabledByFlag() = runServerTest {
        ServerFlags.set { it.copy(translations = false) }
        installRouting()
        val client = createClient(token = null)

        assertEquals(expected = HttpStatusCode.NotFound, actual = client.get(request = HttpRequest(url = URL.Translations)).status)
    }

    /** Verifies that the translations endpoint responds with an internal server error when the database fails. */
    @Test
    fun getTranslationsDatabaseFailure() = runServerTest {
        installRouting()
        val client = createClient(token = null)
        dependency.get().database.reset()

        assertEquals(expected = HttpStatusCode.InternalServerError, actual = client.get(request = HttpRequest(url = URL.Translations)).status)
    }

    /** Verifies that updating a translation requires authentication, translation write permission and a valid body, and returns the stored translation. */
    @Test
    fun updateTranslation() = runServerTest {
        installRouting()
        val request = HttpRequest(url = URL.Translations)
        val edited = FakeData.translations.first().copy(modifiedAt = now(), value = "edited")
        assertEquals(expected = Outcome.Success(data = Unit), actual = dependency.get().useCases.translationUseCases.seedTranslations(translations = FakeData.translations))

        assertEquals(expected = HttpStatusCode.Unauthorized, actual = createClient(token = null).post(request = request, body = edited).status)

        // FakeData.account only has TRANSLATION -> READ.
        assertEquals(expected = HttpStatusCode.Forbidden, actual = createClient(token = bearerToken.accessToken).post(request = request, body = edited).status)

        val adminClient = createClient(token = adminBearerToken.accessToken)
        assertEquals(expected = HttpStatusCode.BadRequest, actual = adminClient.post(request = request, body = "invalid body").status)

        val response = adminClient.post(request = request, body = edited)
        assertEquals(expected = HttpStatusCode.OK, actual = response.status)
        assertEquals(expected = edited, actual = response.body<Translation>())
        assertEquals(
            expected = (FakeData.translations.drop(n = 1) + edited).toSet(),
            actual = adminClient.get(request = request).body<List<Translation>>().toSet()
        )
    }

    /** Verifies that updating a translation whose language and key are not stored responds not found. */
    @Test
    fun updateUnknownTranslation() = runServerTest {
        installRouting()
        val client = createClient(token = adminBearerToken.accessToken)
        val unknown = FakeData.translations.first().copy(key = "unknown_key", modifiedAt = now())
        assertEquals(expected = Outcome.Success(data = Unit), actual = dependency.get().useCases.translationUseCases.seedTranslations(translations = FakeData.translations))

        assertEquals(expected = HttpStatusCode.NotFound, actual = client.post(request = HttpRequest(url = URL.Translations), body = unknown).status)
    }

    /** Verifies that an update older than the stored translation responds with the stored translation. */
    @Test
    fun updateOlderTranslation() = runServerTest {
        installRouting()
        val client = createClient(token = adminBearerToken.accessToken)
        val request = HttpRequest(url = URL.Translations)
        val now = now()
        val newer = FakeData.translations.first().copy(modifiedAt = now, value = "newer")
        val older = newer.copy(modifiedAt = now - 1.seconds, value = "older")
        assertEquals(expected = Outcome.Success(data = Unit), actual = dependency.get().useCases.translationUseCases.seedTranslations(translations = FakeData.translations))
        assertEquals(expected = HttpStatusCode.OK, actual = client.post(request = request, body = newer).status)

        val response = client.post(request = request, body = older)

        assertEquals(expected = HttpStatusCode.OK, actual = response.status)
        assertEquals(expected = newer, actual = response.body<Translation>())
    }

    /** Verifies that updating a translation responds not found when disabled by flag. */
    @Test
    fun updateTranslationIsDisabledByFlag() = runServerTest {
        ServerFlags.set { it.copy(translations = false) }
        installRouting()
        val client = createClient(token = adminBearerToken.accessToken)

        assertEquals(expected = HttpStatusCode.NotFound, actual = client.post(request = HttpRequest(url = URL.Translations), body = FakeData.translations.first()).status)
    }

    /** Verifies that updating a translation broadcasts a translations payload to the connected SSE clients. */
    @Test
    fun updateTranslationBroadcasts() = runServerTest {
        ServerFlags.set { it.copy(broadcast = true) }
        installRouting()
        val client = createClient(token = null)
        val adminClient = createClient(token = adminBearerToken.accessToken)
        val edited = FakeData.translations.first().copy(modifiedAt = now(), value = "edited")
        assertEquals(expected = Outcome.Success(data = Unit), actual = dependency.get().useCases.translationUseCases.seedTranslations(translations = FakeData.translations))

        var received: PushPayload? = null
        client.sse(
            request = HttpRequest(url = URL.Broadcast),
            onConnected = { launch { adminClient.post(request = HttpRequest(url = URL.Translations), body = edited) } }
        ) { event ->
            received = event.data?.let { decode<PushPayload>(value = it) } ?: return@sse
            cancel()
        }

        assertIs<PushPayload.Translations>(value = received)
    }
}
