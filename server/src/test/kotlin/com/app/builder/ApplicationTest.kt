package com.app.builder

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Instant
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import com.app.builder.core.config.ServerConfigs
import com.app.builder.core.locale.UNIX_EPOCH
import com.app.builder.data.database.create
import com.app.builder.data.database.reset
import com.app.builder.domain.usecase.getOrNull
import com.app.builder.test.TestCase

class ApplicationTest: TestCase() {

    /** Verifies the root endpoint responds with the running status message. */
    @Test
    fun app() = runServerTest {
        application { module() }

        assertEquals(expected = "Server running", actual = client.get(urlString = "/").bodyAsText())
    }

    /** Verifies that starting the application seeds the translations. */
    @Test
    fun appSeedsTranslations() = runServerTest {
        application { module() }
        startApplication()

        assertTrue(actual = dependency.get().useCases.translationUseCases.getTranslations().getOrNull().orEmpty().isNotEmpty())
    }

    /** Verifies that starting the application in development mode seeds the entities of the seed resource. */
    @Test
    fun appSeedsEntities() = runServerTest {
        application { module() }
        startApplication()

        val useCases = dependency.get().useCases
        assertTrue(actual = useCases.buildingUseCases.getBuildings(lastSyncUtc = Instant.UNIX_EPOCH, cursorUtc = null, cursorUuid = null, pageSize = Int.MAX_VALUE).getOrNull().orEmpty().isNotEmpty())
        assertTrue(actual = registries().isNotEmpty())
    }

    /** Verifies that the application fails to start when the seed fails, here because the admin has no account and its password is shorter than the minimum length. */
    @Test
    fun appFailsWhenSeedFails() = runServerTest {
        dependency.get().database.reset()
        dependency.get().database.create()
        ServerConfigs.set { it.copy(minPasswordLength = Int.MAX_VALUE) }
        application { module() }

        assertFailsWith<IllegalStateException> { startApplication() }
    }
}
