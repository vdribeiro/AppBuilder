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
    ACCOUNT,
    USER,
    DEVICE_LOCATION,
    NFC,
    TASK,
    EQUIPMENT,
    BUILDING,
    BUILDING_EQUIPMENT;

    companion object {
        /** The entity types whose permission is granted to accounts, leaving out those derived from other entity types. */
        val assignable: List<EntityType> = entries - BUILDING_EQUIPMENT
    }
}