package com.app.builder.data.database.table

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

/** Table for storing live WebSocket connection presence. */
object ConnectionTable: Table(name = "connection") {
    /** The unique identifier of the connected device. */
    val deviceUuid = uuid(name = "device_uuid")
    /** The unique identifier of the user who owns the connection. */
    val userUuid = reference(name = "user_uuid", refColumn = UserTable.uuid)
    /** The unique identifier of the server instance handling this connection. */
    val instanceId = uuid(name = "instance_id")
    /** The timestamp of the last presence heartbeat for this connection. */
    val updatedAt = timestamp(name = "updated_at")

    override val primaryKey = PrimaryKey(firstColumn = deviceUuid)
}
