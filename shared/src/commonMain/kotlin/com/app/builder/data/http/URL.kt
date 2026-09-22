package com.app.builder.data.http

/**
 * A type-safe representation of all remote endpoints used within the application, serving as a centralized registry for API routing.
 *
 * @property path The fully qualified URL string for the targeted network request.
 */
sealed class URL(val path: String) {

    companion object {
        /** The production environment host. */
        const val HOST = "server-670810444998.europe-west1.run.app"
        /** The local development port. */
        const val DEV_PORT = 8080
        /** The local development base host. */
        const val DEV_HOST = "localhost:$DEV_PORT"
    }

    /** Endpoint for server flags. */
    data object ServerFlags: URL(path = "/api/serverflags")
    /** Endpoint for server configs. */
    data object ServerConfigs: URL(path = "/api/serverconfigs")
    /** Endpoint for client feature flags. */
    data object ClientFlags: URL(path = "/api/clientflags")
    /** Endpoint for client configs. */
    data object ClientConfigs: URL(path = "/api/clientconfigs")
    /** Endpoint for probing server. */
    data object Probe: URL(path = "/api/probe")
    /** Endpoint for registries. */
    data object Registries: URL(path = "/api/registry")
    /** Endpoint to retrieve the static translation JSON payload. */
    data object Translations: URL(path = "/api/data/translations.json")
    /** Endpoint to register a device's FCM token for push delivery. */
    data object DeviceTokens: URL(path = "/api/device-tokens")
    /** Endpoint to subscribe a device's FCM token to the broadcast topic. */
    data object BroadcastSubscribe: URL(path = "/api/broadcast/subscribe")
    /** Endpoint for the public SSE broadcast stream, and for publishing a broadcast to every connected client. */
    data object Broadcast: URL(path = "/api/broadcast")
    /** Endpoint to get short-lived one time tickets for authenticated connections. */
    data object Tickets: URL(path = "/api/tickets")
    /** Endpoint for push payloads. */
    data object Push: URL(path = "/api/push")
    /** Endpoint to register a new user account. */
    data object Register: URL(path = "/api/register")
    /** Endpoint to authenticate an existing user and obtain session tokens. */
    data object Login: URL(path = "/api/login")
    /** Endpoint to invalidate the current session. */
    data object Logout: URL(path = "/api/logout")
    /** Endpoint to exchange an expired access token for a renewed session token pair. */
    data object RefreshTokens: URL(path = "/api/refresh-tokens")
    /** Endpoint for users. */
    data object Users: URL(path = "/api/users")
    /** Endpoint to report a device location fix. */
    data object DeviceLocations: URL(path = "/api/device-location")
    /** Endpoint for tasks. */
    data object Tasks: URL(path = "/api/tasks")
}