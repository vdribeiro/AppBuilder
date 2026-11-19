package com.app.builder.data.database.table

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

/** Table for storing translations. */
object TranslationTable: Table(name = "translation") {
    /** The ISO language code of the translation. */
    val languageIso = text(name = "language_iso")
    /** The identifier of the translation within its language. */
    val key = text(name = "key")
    /** The timestamp indicating when this translation was last updated. */
    val modifiedAt = timestamp(name = "modified_at")
    /** The localized text of the translation. */
    val value = text(name = "value")

    override val primaryKey = PrimaryKey(languageIso, key)
}
