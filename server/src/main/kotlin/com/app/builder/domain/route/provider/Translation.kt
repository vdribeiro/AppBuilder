package com.app.builder.domain.route.provider

import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.http.URL
import com.app.builder.domain.route.getOrRespond
import com.app.builder.domain.route.respondSafely
import com.app.builder.domain.usecase.translation.TranslationUseCases

/**
 * Configures the translation routes.
 *
 * @param translationUseCases The collection of use cases handling translation logic.
 */
fun Route.translationRoutes(translationUseCases: TranslationUseCases) {
    get(path = URL.Translations.path) {
        val translations = call.getOrRespond(outcome = translationUseCases.getTranslations(), error = "Error getting translations") ?: return@get
        call.respondSafely(status = HttpStatusCode.OK, message = translations)
        Telemetry.info(tag = TAG, message = "Fetched ${translations.size} translations")
    }
}

private const val TAG = "TranslationRoute"
