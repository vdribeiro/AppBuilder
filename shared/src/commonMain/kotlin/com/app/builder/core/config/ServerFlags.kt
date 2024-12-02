package com.app.builder.core.config

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.serialization.Serializable

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
    val tasks: Boolean
) {
    companion object {

        /** Default feature flags. */
        private val default = ServerFlags(
            resources = true,
            probe = true,
            authentication = true,
            broadcast = true,
            push = true,
            firebase = true,
            users = true,
            deviceLocation = true,
            tasks = true
        )

        /** Backing state flow tracking feature flag changes. */
        private val _flagsFlow: MutableStateFlow<ServerFlags> = MutableStateFlow(value = default)
        /** Current synchronous snapshot of the feature flags. */
        val flags: ServerFlags get() = _flagsFlow.value

        /**
         * Atomically mutates the feature configuration state block and returns the freshly evaluated state.
         *
         * @param flags A lambda block receiving the current [ServerFlags] snapshot and returning a modified copy.
         * @return The updated [ServerFlags] instance.
         */
        fun set(flags: (ServerFlags) -> ServerFlags): ServerFlags = _flagsFlow.updateAndGet(function = flags)

        /**
         * Restores the feature flags to their default values.
         *
         * @return The default [ServerFlags] instance.
         */
        fun reset(): ServerFlags = set { default }
    }
}
