package com.app.builder.domain.route

import io.ktor.server.application.Application
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.RouteSelector
import io.ktor.server.routing.RouteSelectorEvaluation
import io.ktor.server.routing.RoutingResolveContext
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import com.app.builder.Dependency
import com.app.builder.core.config.ServerFlags
import com.app.builder.domain.route.provider.accountRoutes
import com.app.builder.domain.route.provider.authenticationRoutes
import com.app.builder.domain.route.provider.broadcastRoutes
import com.app.builder.domain.route.provider.buildingEquipmentRoutes
import com.app.builder.domain.route.provider.buildingMaterialRoutes
import com.app.builder.domain.route.provider.buildingRoutes
import com.app.builder.domain.route.provider.configRoutes
import com.app.builder.domain.route.provider.deviceLocationRoutes
import com.app.builder.domain.route.provider.equipmentRoutes
import com.app.builder.domain.route.provider.materialRoutes
import com.app.builder.domain.route.provider.probeRoutes
import com.app.builder.domain.route.provider.pushRoutes
import com.app.builder.domain.route.provider.registryRoutes
import com.app.builder.domain.route.provider.taskRoutes
import com.app.builder.domain.route.provider.translationRoutes
import com.app.builder.domain.route.provider.userRoutes

/**
 * Sets all routes.
 *
 * @param dependency The application dependency graph.
 */
fun Application.installRouting(dependency: Dependency) {
    val permissionService = dependency.permissionService
    val useCases = dependency.useCases
    routing {
        get(path = "/") { call.respondText(text = "Server running") }
        registryRoutes(permissionService = permissionService, registryUseCases = useCases.registryUseCases)
        configRoutes(permissionService = permissionService, configUseCases = useCases.configUseCases, registryUseCases = useCases.registryUseCases)
        flag(enabled = { it.translations }) { translationRoutes(permissionService = permissionService, translationUseCases = useCases.translationUseCases, registryUseCases = useCases.registryUseCases) }
        flag(enabled = { it.probe }) { probeRoutes() }
        flag(enabled = { it.authentication }) { authenticationRoutes(permissionService = permissionService, authenticationUseCases = useCases.authenticationUseCases, registryUseCases = useCases.registryUseCases) }
        flag(enabled = { it.broadcast }) { broadcastRoutes(permissionService = permissionService, broadcastService = dependency.broadcastService, fcmService = dependency.fcmService, registryUseCases = useCases.registryUseCases) }
        flag(enabled = { it.push }) { pushRoutes(permissionService = permissionService, deviceTokenUseCases = useCases.deviceTokenUseCases, pushService = dependency.pushService, registryUseCases = useCases.registryUseCases) }
        flag(enabled = { it.users }) { userRoutes(permissionService = permissionService, userUseCases = useCases.userUseCases, accountUseCases = useCases.accountUseCases, registryUseCases = useCases.registryUseCases) }
        flag(enabled = { it.users }) { accountRoutes(permissionService = permissionService, accountUseCases = useCases.accountUseCases, registryUseCases = useCases.registryUseCases) }
        flag(enabled = { it.deviceLocation }) { deviceLocationRoutes(permissionService = permissionService, deviceLocationUseCases = useCases.deviceLocationUseCases, registryUseCases = useCases.registryUseCases) }
        flag(enabled = { it.tasks }) { taskRoutes(permissionService = permissionService, taskUseCases = useCases.taskUseCases, registryUseCases = useCases.registryUseCases) }
        flag(enabled = { it.equipments }) { equipmentRoutes(permissionService = permissionService, equipmentUseCases = useCases.equipmentUseCases, registryUseCases = useCases.registryUseCases) }
        flag(enabled = { it.buildings }) { buildingRoutes(permissionService = permissionService, buildingUseCases = useCases.buildingUseCases, registryUseCases = useCases.registryUseCases) }
        flag(enabled = { it.buildingEquipments }) { buildingEquipmentRoutes(permissionService = permissionService, buildingEquipmentUseCases = useCases.buildingEquipmentUseCases, registryUseCases = useCases.registryUseCases) }
        flag(enabled = { it.materials }) { materialRoutes(permissionService = permissionService, materialUseCases = useCases.materialUseCases, registryUseCases = useCases.registryUseCases) }
        flag(enabled = { it.buildingMaterials }) { buildingMaterialRoutes(permissionService = permissionService, buildingMaterialUseCases = useCases.buildingMaterialUseCases, registryUseCases = useCases.registryUseCases) }
    }
}

/**
 * Registers routes that match only while their server feature flag is on.
 *
 * @param enabled Returns if the routes are enabled for the given [ServerFlags].
 * @param build Registers the routes.
 */
private fun Route.flag(enabled: (ServerFlags) -> Boolean, build: Route.() -> Unit) {
    createChild(selector = FlagRouteSelector(enabled = enabled)).apply(block = build)
}

/**
 * A [RouteSelector] that matches only while a server feature flag is on.
 *
 * @property enabled Returns if the routes are enabled for the given [ServerFlags].
 */
private class FlagRouteSelector(
    private val enabled: (ServerFlags) -> Boolean
): RouteSelector() {

    override suspend fun evaluate(context: RoutingResolveContext, segmentIndex: Int): RouteSelectorEvaluation =
        if (enabled(ServerFlags.value)) RouteSelectorEvaluation.Transparent else RouteSelectorEvaluation.Failed
}
