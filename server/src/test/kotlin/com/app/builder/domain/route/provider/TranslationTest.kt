package com.app.builder.domain.route.provider

import kotlin.test.Test
import kotlin.test.assertEquals
import io.ktor.client.call.body
import io.ktor.http.HttpStatusCode
import com.app.builder.core.config.ServerFlags
import com.app.builder.data.database.reset
import com.app.builder.data.http.HttpRequest
import com.app.builder.data.http.URL
import com.app.builder.data.http.get
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
}
