package com.app.builder.data.database.table

import org.jetbrains.exposed.v1.core.Table

/** Table for storing device tokens. */
object DeviceTokenTable: Table(name = "device_token") {
    /** The unique identifier of the device the token belongs to. */
    val deviceUuid = uuid(name = "device_uuid")
    /** The unique identifier of the user who owns the device. */
    val userUuid = reference(name = "user_uuid", refColumn = UserTable.uuid)
    /** The FCM registration token for the device. */
    val token = text(name = "token")

    override val primaryKey = PrimaryKey(firstColumn = deviceUuid)
}
