package com.app.builder.core.config

import kotlinx.serialization.Serializable
import com.app.builder.core.flow.StateHolder

/** Centralized feature flag container to change system capabilities dynamically. */
@Serializable
data class ClientFlags(
    /** Toggles console/stacktrace viewer. */
    val console: Boolean,
    /** Toggles outbound networking. When false, every HTTP request throws immediately instead of being sent. This is a kill switch for all client network traffic. */
    val http: Boolean,
    /** Toggles the offline scheduler's background processing loop. When false, jobs are no longer queued for later, they execute immediately instead, so sync work still happens, just synchronously rather than as durable background jobs. */
    val scheduler: Boolean,
    /** Toggles background and ambient playback within the audio player. When false, playback actions are ignored and any active playback is force-stopped. */
    val music: Boolean,
    /** Toggles the camera capture feature. When false, any in-progress recording/preview is stopped and reset, and further capture actions are dropped. */
    val camera: Boolean,
    /** Toggles the NFC feature. When false, any in-progress scan/write session is stopped and reset, and further scan/write actions are dropped. */
    val nfc: Boolean,
    /** Toggles the broadcast SSE stream. When false, the stream is not opened. */
    val broadcastStream: Boolean,
    /** Toggles the broadcast Firebase topic subscription. When false, the push token is not subscribed to broadcasts. */
    val broadcastFirebase: Boolean,
    /** Toggles the push WebSocket. When false, no ticket is requested and the socket is not opened. */
    val pushSocket: Boolean,
    /** Toggles the Firebase token registration. When false, the push token is not registered. */
    val pushFirebase: Boolean,
    /** Toggles the data resync triggered by an incoming push notification, whichever channel it arrives on. The OS notification itself is always shown, only the follow-up entity resync is skipped when false. */
    val pushSync: Boolean,
    /** Toggles the location capture feature. When false, no locations are collected. */
    val locationCapture: Boolean,
    /** Toggles the scrollbar on screens it is not enforced. */
    val scrollBar: Boolean,
    /** Toggles the design section screens, showcasing the design system's components, icons, colors, shapes and typography. When false, the screens are not reachable and their navigation item is hidden. */
    val design: Boolean,
    /** Toggles the Home screen. When false, falls back to the error screen. */
    val home: Boolean,
    /** Toggles the Login screen. When false, falls back to the error screen. */
    val login: Boolean,
    /** Toggles the screen for managing push notifications, and whether the notification permission may be granted. When false, the screen falls back to the error screen and any request for the notification permission is refused. */
    val notifications: Boolean,
    /** Toggles the Device Files screen, listing the files the application holds on the device. When false, the screen is not reachable and its navigation item is hidden. */
    val deviceFiles: Boolean,
    /** Toggles the User Profile screen. When false, falls back to the error screen. */
    val userProfile: Boolean,
    /** Toggles the Help screen. When false, the screen falls back to the error screen and its entry is hidden from the User Profile screen. */
    val help: Boolean,
    /** Toggles the user screens. When false, falls back to the error screen. */
    val users: Boolean,
    /** Toggles the registry screens and whether registry entities are queued for background sync. When false, registry screens fall back to the error screen, their navigation item is hidden, and registry data is excluded from sync even if the user has permission to read it. */
    val registries: Boolean,
    /** Toggles the task screens and whether task entities are queued for background sync. When false, task screens fall back to the error screen and task data is excluded from sync even if the user has permission to read it. */
    val tasks: Boolean,
    /** Toggles the time punch screens and whether time punch entities are queued for background sync. When false, time punch screens fall back to the error screen, their navigation item is hidden, and time punch data is excluded from sync. */
    val timePunches: Boolean,
    /** Toggles the equipment screens and whether equipment entities are queued for background sync. When false, equipment screens fall back to the error screen and equipment data is excluded from sync even if the user has permission to read it. */
    val equipments: Boolean,
    /** Toggles the building screens and whether building entities are queued for background sync. When false, building screens fall back to the error screen and building data is excluded from sync even if the user has permission to read it. */
    val buildings: Boolean,
    /** Toggles the building equipment sections of the building and equipment screens and whether building equipment entities are queued for background sync. When false, the sections are hidden and building equipment data is excluded from sync even if the user has permission to read it. */
    val buildingEquipments: Boolean,
    /** Toggles the material screens and whether material entities are queued for background sync. When false, material screens fall back to the error screen and material data is excluded from sync even if the user has permission to read it. */
    val materials: Boolean,
    /** Toggles the building material sections of the building and material screens and whether building material entities are queued for background sync. When false, the sections are hidden and building material data is excluded from sync even if the user has permission to read it. */
    val buildingMaterials: Boolean,
    /** Toggles the building user sections of the building and user screens and whether building user entities are queued for background sync. When false, the sections are hidden and building user data is excluded from sync even if the user has permission to read it. */
    val buildingUsers: Boolean,
    /** Toggles the equipment user sections of the equipment and user screens and whether equipment user entities are queued for background sync. When false, the sections are hidden and equipment user data is excluded from sync even if the user has permission to read it. */
    val equipmentUsers: Boolean,
) {
    /** Holds the client feature flags. */
    companion object: StateHolder<ClientFlags>(
        initialValue = ClientFlags(
            console = true,
            http = true,
            scheduler = true,
            music = true,
            camera = true,
            nfc = true,
            broadcastStream = true,
            broadcastFirebase = true,
            pushSocket = true,
            pushFirebase = true,
            pushSync = true,
            locationCapture = true,
            scrollBar = true,
            design = true,
            home = true,
            login = true,
            notifications = true,
            deviceFiles = true,
            userProfile = true,
            help = true,
            users = true,
            registries = true,
            tasks = true,
            timePunches = true,
            equipments = true,
            buildings = true,
            buildingEquipments = true,
            materials = true,
            buildingMaterials = true,
            buildingUsers = true,
            equipmentUsers = true,
        )
    ) {
        /**
         * Sets every flag to false.
         *
         * @return The flags with every value false.
         */
        fun off(): ClientFlags = set {
            ClientFlags(
                console = false,
                http = false,
                scheduler = false,
                music = false,
                camera = false,
                nfc = false,
                broadcastStream = false,
                broadcastFirebase = false,
                pushSocket = false,
                pushFirebase = false,
                pushSync = false,
                locationCapture = false,
                scrollBar = false,
                design = false,
                home = false,
                login = false,
                notifications = false,
                deviceFiles = false,
                userProfile = false,
                help = false,
                users = false,
                registries = false,
                tasks = false,
                timePunches = false,
                equipments = false,
                buildings = false,
                buildingEquipments = false,
                materials = false,
                buildingMaterials = false,
                buildingUsers = false,
                equipmentUsers = false,
            )
        }
    }
}
