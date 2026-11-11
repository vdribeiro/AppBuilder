package com.app.builder.core.config

import kotlinx.serialization.Serializable
import com.app.builder.core.flow.StateHolder

/** Centralized feature flag container to change system capabilities dynamically. */
@Serializable
data class ServerFlags(
    /** Toggles the static file mount. When false, requests to it return a 404. */
    val resources: Boolean,
    /** Toggles the health check endpoint. When false, the route is not registered and returns 404. */
    val probe: Boolean,
    /** Toggles session/credential routes. When false, all of these return 404. */
    val authentication: Boolean,
    /** Toggles the broadcast SSE route and the manager that fans out cross-instance signals to connected sessions. When false, the route return 404 and the manager never starts listening. */
    val broadcast: Boolean,
    /** Toggles the push WebSocket ticket/connect routes and the server's push delivery service. When false, the routes return 404 and no push payloads are ever delivered. */
    val push: Boolean,
    /** Toggles Firebase initialization. When false, Firebase services become no-ops. */
    val firebase: Boolean,
    /** Toggles user CRUD routes. When false, they return 404. */
    val users: Boolean,
    /** Toggles the device location ingestion route. When false, it returns 404. */
    val deviceLocation: Boolean,
    /** Toggles task CRUD routes. When false, they return 404. */
    val tasks: Boolean,
    /** Toggles equipment CRUD routes. When false, they return 404. */
    val equipments: Boolean,
    /** Toggles building CRUD routes. When false, they return 404. */
    val buildings: Boolean
) {
    /** Holds the server feature flags. */
    companion object: StateHolder<ServerFlags>(
        default = ServerFlags(
            resources = true,
            probe = true,
            authentication = true,
            broadcast = true,
            push = true,
            firebase = true,
            users = true,
            deviceLocation = true,
            tasks = true,
            equipments = true,
            buildings = true
        )
    )
}
