package com.app.builder.data.database.table

import org.jetbrains.exposed.v1.core.Table

/** Table for storing feature flags and configs. */
object ConfigTable: Table(name = "config") {
    /** The identifier of the config or feature-flag set this row stores. */
    val key = enumeration<Key>(name = "key")
    /** The JSON-encoded value of the config or feature-flag set. */
    val value = text(name = "value")

    override val primaryKey = PrimaryKey(firstColumn = key)

    /** Identifies which config or feature-flag set a [ConfigTable] row stores. */
    enum class Key {
        CLIENT_FLAG,
        CLIENT_CONFIG,
        SERVER_FLAG,
        SERVER_CONFIG
    }
}
