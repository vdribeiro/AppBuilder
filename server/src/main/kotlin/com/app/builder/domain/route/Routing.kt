package com.app.builder.domain.route

import io.ktor.server.application.Application
import io.ktor.server.http.content.staticResources
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import com.app.builder.Dependency
import com.app.builder.core.config.ServerFlags
import com.app.builder.domain.route.provider.authenticationRoutes
import com.app.builder.domain.route.provider.broadcastRoutes
import com.app.builder.domain.route.provider.configRoutes
import com.app.builder.domain.route.provider.deviceLocationRoutes
import com.app.builder.domain.route.provider.probeRoutes
import com.app.builder.domain.route.provider.pushRoutes
import com.app.builder.domain.route.provider.registryRoutes
import com.app.builder.domain.route.provider.taskRoutes
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
        if (ServerFlags.flags.resources) staticResources(remotePath = "/api/data", basePackage = "static")
        if (ServerFlags.flags.probe) probeRoutes()
        if (ServerFlags.flags.authentication) authenticationRoutes(permissionService = permissionService, authenticationUseCases = useCases.authenticationUseCases, registryUseCases = useCases.registryUseCases)
        if (ServerFlags.flags.broadcast) broadcastRoutes(permissionService = permissionService, broadcastService = dependency.broadcastService, fcmService = dependency.fcmService, registryUseCases = useCases.registryUseCases)
        if (ServerFlags.flags.push) pushRoutes(permissionService = permissionService, deviceTokenUseCases = useCases.deviceTokenUseCases, pushService = dependency.pushService, registryUseCases = useCases.registryUseCases)
        if (ServerFlags.flags.users) userRoutes(permissionService = permissionService, userUseCases = useCases.userUseCases, registryUseCases = useCases.registryUseCases)
        if (ServerFlags.flags.deviceLocation) deviceLocationRoutes(permissionService = permissionService, deviceLocationUseCases = useCases.deviceLocationUseCases, registryUseCases = useCases.registryUseCases)
        if (ServerFlags.flags.tasks) taskRoutes(permissionService = permissionService, taskUseCases = useCases.taskUseCases, registryUseCases = useCases.registryUseCases)
    }
}
