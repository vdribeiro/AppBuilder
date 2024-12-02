package com.app.builder.data.database.adapter

import kotlin.time.Instant
import app.cash.sqldelight.ColumnAdapter

/** Column adapter for Instant. */
object InstantColumnAdapter: ColumnAdapter<Instant, String> {
    override fun decode(databaseValue: String): Instant = Instant.parse(input = databaseValue)
    override fun encode(value: Instant): String = value.toString()
}