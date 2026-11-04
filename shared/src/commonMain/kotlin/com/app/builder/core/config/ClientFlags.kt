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
    /** Toggles the broadcast listener. When false, no broadcasts are received. */
    val broadcast: Boolean,
    /** Toggles the push service. When false, no push payloads are received. */
    val push: Boolean,
    /** Toggles the data resync triggered by an incoming push notification. Only takes effect if [push] is also enabled. The OS notification itself is always shown, only the follow-up entity resync is skipped when false. */
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
    /** Toggles the user screens. When false, falls back to the error screen. */
    val users: Boolean,
    /** Toggles the registry screens and whether registry entities are queued for background sync. When false, registry screens fall back to the error screen, their navigation item is hidden, and registry data is excluded from sync even if the user has permission to read it. */
    val registries: Boolean,
    /** Toggles the task screens and whether task entities are queued for background sync. When false, task screens fall back to the error screen and task data is excluded from sync even if the user has permission to read it. */
    val tasks: Boolean,
) {
    /** Holds the client feature flags. */
    companion object: StateHolder<ClientFlags>(
        default = ClientFlags(
            console = true,
            http = true,
            scheduler = true,
            music = true,
            camera = true,
            nfc = true,
            broadcast = true,
            push = true,
            pushSync = true,
            locationCapture = false,
            scrollBar = true,
            design = true,
            home = true,
            login = true,
            notifications = true,
            deviceFiles = true,
            userProfile = true,
            users = true,
            registries = true,
            tasks = true,
        )
    )
}
