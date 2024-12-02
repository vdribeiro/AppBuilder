package com.app.builder.data.database.adapter

import kotlin.uuid.Uuid
import app.cash.sqldelight.ColumnAdapter
import com.app.builder.core.security.toUuid

/** Column adapter for UUIDs. */
object UuidColumnAdapter: ColumnAdapter<Uuid, String> {
    override fun decode(databaseValue: String): Uuid = databaseValue.toUuid()!!
    override fun encode(value: Uuid): String = value.toString()
}