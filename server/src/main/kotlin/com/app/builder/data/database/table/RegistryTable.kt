package com.app.builder.data.database.table

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

/** Table for storing a registry of incoming requests. */
object RegistryTable: Table(name = "registry") {
    /** The unique identifier of the request. */
    val requestUuid = uuid(name = "request_uuid")
    /** The HTTP method used for the request. */
    val requestType = text(name = "request_type")
    /** The UTC timestamp of when the request was made on the client. */
    val requestUtc = timestamp(name = "request_utc")
    /** The UTC timestamp of when the request was sent by the client. */
    val requestSentUtc = timestamp(name = "request_sent_utc")
    /** The timestamp indicating when this registry was last deleted. */
    val deletedAt = timestamp(name = "deleted_at").nullable()
    /** The unique identifier of the authenticated user who made the request. */
    val userUuid = uuid(name = "user_uuid")
    /** The client application version. */
    val appVersion = text(name = "app_version")
    /** The operating system family of the client device. */
    val os = text(name = "os")
    /** The operating system version of the client device. */
    val osVersion = text(name = "os_version")
    /** The manufacturer or brand of the client device. */
    val brand = text(name = "brand")
    /** The hardware model of the client device. */
    val model = text(name = "model")
    /** The client device's installation UUID. */
    val deviceUuid = uuid(name = "device_uuid")
    /** The unique identifier of the entity targeted by the request, if any. */
    val entityUuid = uuid(name = "entity_uuid").nullable()
    /** The type of the entity targeted by the request, if any. */
    val entityType = text(name = "entity_type").nullable()
    /** The request body, if captured. */
    val payload = text(name = "payload").nullable()

    override val primaryKey = PrimaryKey(firstColumn = requestUuid)
}
