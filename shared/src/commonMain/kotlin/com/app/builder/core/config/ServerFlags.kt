package com.app.builder.core.config

import kotlinx.serialization.Serializable
import com.app.builder.core.flow.StateHolder

/** Centralized feature flag container to change system capabilities dynamically. */
@Serializable
data class ServerFlags(
    /** Toggles the translations route. When false, it returns 404. */
    val translations: Boolean,
    /** Toggles the health check endpoint. When false, the route is not registered and returns 404. */
    val probe: Boolean,
    /** Toggles session/credential routes. When false, all of these return 404. */
    val authentication: Boolean,
    /** Toggles the broadcast SSE route and the manager that fans out cross-instance signals to connected sessions. When false, the route is not served, open streams end and the manager stops listening. */
    val broadcastStream: Boolean,
    /** Toggles the broadcast Firebase topic and its subscribe route. When false, the route returns 404 and sending a broadcast to the topic is a no-op. */
    val broadcastFirebase: Boolean,
    /** Toggles the push WebSocket ticket/connect routes, the connection presence heartbeat and the per-user notification observers. When false, the routes are not served and open sockets close. */
    val pushSocket: Boolean,
    /** Toggles push delivery over Firebase and the device token route. When false, the route returns 404 and sending a push payload to device tokens is a no-op. */
    val pushFirebase: Boolean,
    /** Toggles user CRUD routes. When false, they return 404. */
    val users: Boolean,
    /** Toggles the device location ingestion route. When false, it returns 404. */
    val deviceLocation: Boolean,
    /** Toggles task CRUD routes. When false, they return 404. */
    val tasks: Boolean,
    /** Toggles time punch CRUD routes. When false, they return 404. */
    val timePunches: Boolean,
    /** Toggles equipment CRUD routes. When false, they return 404. */
    val equipments: Boolean,
    /** Toggles building CRUD routes. When false, they return 404. */
    val buildings: Boolean,
    /** Toggles building equipment CRUD routes. When false, they return 404. */
    val buildingEquipments: Boolean,
    /** Toggles material CRUD routes. When false, they return 404. */
    val materials: Boolean,
    /** Toggles building material CRUD routes. When false, they return 404. */
    val buildingMaterials: Boolean,
    /** Toggles building user CRUD routes. When false, they return 404. */
    val buildingUsers: Boolean,
    /** Toggles equipment user CRUD routes. When false, they return 404. */
    val equipmentUsers: Boolean,
    /** Toggles the file upload and download routes. When false, they return 404. */
    val files: Boolean
) {
    /** Holds the server feature flags. */
    companion object: StateHolder<ServerFlags>(
        initialValue = ServerFlags(
            translations = true,
            probe = true,
            authentication = true,
            broadcastStream = true,
            broadcastFirebase = true,
            pushSocket = true,
            pushFirebase = true,
            users = true,
            deviceLocation = true,
            tasks = true,
            timePunches = true,
            equipments = true,
            buildings = true,
            buildingEquipments = true,
            materials = true,
            buildingMaterials = true,
            buildingUsers = true,
            equipmentUsers = true,
            files = false
        )
    ) {
        /**
         * Sets every flag to false.
         *
         * @return The flags with every value false.
         */
        fun off(): ServerFlags = set {
            ServerFlags(
                translations = false,
                probe = false,
                authentication = false,
                broadcastStream = false,
                broadcastFirebase = false,
                pushSocket = false,
                pushFirebase = false,
                users = false,
                deviceLocation = false,
                tasks = false,
                timePunches = false,
                equipments = false,
                buildings = false,
                buildingEquipments = false,
                materials = false,
                buildingMaterials = false,
                buildingUsers = false,
                equipmentUsers = false,
                files = false
            )
        }
    }
}
