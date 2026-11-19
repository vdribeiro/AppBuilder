package com.app.builder.domain.route.provider

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import com.app.builder.core.config.ServerFlags
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.http.URL
import com.app.builder.data.serializer.encode
import com.app.builder.domain.EntityType
import com.app.builder.domain.Permission
import com.app.builder.domain.PushPayload
import com.app.builder.domain.Translation
import com.app.builder.domain.permission.PermissionService
import com.app.builder.domain.push.BroadcastService
import com.app.builder.domain.route.getOrRespond
import com.app.builder.domain.route.getUserUuid
import com.app.builder.domain.route.receive
import com.app.builder.domain.route.respondSafely
import com.app.builder.domain.route.toRegistry
import com.app.builder.domain.route.validatePermission
import com.app.builder.domain.route.validateTime
import com.app.builder.domain.usecase.registry.RegistryUseCases
import com.app.builder.domain.usecase.translation.TranslationUseCases

/**
 * Configures the translation routes.
 *
 * @param permissionService The permission service.
 * @param translationUseCases The collection of use cases handling translation logic.
 * @param broadcastService The service broadcasting a [PushPayload.Translations] after each update, while the broadcast server flag is on.
 * @param registryUseCases The registry use cases used to record request telemetry.
 */
fun Route.translationRoutes(
    permissionService: PermissionService,
    translationUseCases: TranslationUseCases,
    broadcastService: BroadcastService,
    registryUseCases: RegistryUseCases
) {
    get(path = URL.Translations.path) {
        val translations = call.getOrRespond(outcome = translationUseCases.getTranslations(), error = "Error getting translations") ?: return@get
        call.respondSafely(status = HttpStatusCode.OK, message = translations)
        Telemetry.info(tag = TAG, message = "Fetched ${translations.size} translations")
    }

    authenticate {
        post(path = URL.Translations.path) {
            if (!call.validateTime()) return@post
            val userUuid = call.getUserUuid() ?: return@post
            if (!call.validatePermission(permissionService = permissionService, entityType = EntityType.TRANSLATION, permission = Permission.WRITE)) return@post
            val translation = call.receive<Translation>() ?: return@post

            val stored = call.getOrRespond(
                outcome = translationUseCases.updateTranslation(translation = translation),
                notFound = "Translation not found",
                error = "Error updating translation"
            ) ?: return@post

            call.respondSafely(status = HttpStatusCode.OK, message = stored)
            Telemetry.info(tag = TAG, message = "Updated translation ${stored.languageIso}/${stored.key} for user $userUuid")

            if (ServerFlags.value.broadcast) broadcastService.broadcast(pushPayload = PushPayload.Translations())

            call.toRegistry(
                entityType = EntityType.TRANSLATION,
                payload = encode(value = stored)
            )?.let { registryUseCases.upsertRegistry(registry = it) }
        }
    }
}

private const val TAG = "TranslationRoute"
