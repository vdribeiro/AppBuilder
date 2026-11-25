package com.app.builder.data.database.table

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

/** Table for storing file metadata. */
object FileTable: Table(name = "file") {
    /** The unique, immutable identifier for the file. */
    val uuid = uuid(name = "uuid")
    /** The timestamp indicating when this file was last updated. */
    val modifiedAt = timestamp(name = "modified_at")
    /** The timestamp indicating when this file was last deleted. */
    val deletedAt = timestamp(name = "deleted_at").nullable()
    /** The name of the file. */
    val name = text(name = "name")
    /** The media type of the content. */
    val contentType = text(name = "content_type")
    /** The size of the content, in bytes. */
    val size = long(name = "size")

    override val primaryKey = PrimaryKey(firstColumn = uuid)
}
