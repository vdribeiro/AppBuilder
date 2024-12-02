package com.app.builder.domain

import kotlinx.serialization.Serializable

/** Defines the existing entities. */
@Serializable
enum class EntityType {
    CLIENT_FLAG,
    CLIENT_CONFIG,
    SERVER_FLAG,
    SERVER_CONFIG,
    REGISTRY,
    NOTIFICATION,
    TRANSLATION,
    SESSION,
    USER,
    DEVICE_LOCATION,
    TASK,
    FILE,
}