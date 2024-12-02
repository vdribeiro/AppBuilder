package com.app.builder.core.platform

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Metadata snapshot of the current host execution environment.
 *
 * @property os The operating system family.
 * @property osVersion The specific version of the host operating system.
 * @property brand The manufacturer or vendor of the hardware device.
 * @property model The hardware model identifier or system architecture.
 */
@Serializable
data class Platform(
    val os: OS,
    val osVersion: String,
    val brand: String,
    val model: String,
)

/** Supported operating system families. */
@Serializable
sealed interface OS {
    /** The Android operating system. */
    @Serializable
    @SerialName(value = "android")
    data object Android: OS
    /** The iOS operating system. */
    @Serializable
    @SerialName(value = "ios")
    data object Ios: OS
    /** The Windows operating system. */
    @Serializable
    @SerialName(value = "windows")
    data object Windows: OS
    /** The macOS operating system. */
    @Serializable
    @SerialName(value = "mac")
    data object Mac: OS
    /** The Linux operating system. */
    @Serializable
    @SerialName(value = "linux")
    data object Linux: OS
    /** A web browser runtime. */
    @Serializable
    @SerialName(value = "web")
    data object Web: OS
    /** An operating system that could not be identified. */
    @Serializable
    @SerialName(value = "unknown")
    data object Unknown: OS

    companion object {

        /**
         * Resolves an [OS] entry from its lowercase, human-readable name.
         *
         * @param value The operating system name to parse, matched case-insensitively.
         * @return The matching [OS] entry, or [Unknown] if [value] does not match a known operating system.
         */
        fun fromString(value: String): OS = when (value.lowercase()) {
            "android" -> Android
            "ios" -> Ios
            "windows" -> Windows
            "mac" -> Mac
            "linux" -> Linux
            "web" -> Web
            else -> Unknown
        }
    }
}

/** The runtime [Platform] metadata for the current target. */
expect val platform: Platform
