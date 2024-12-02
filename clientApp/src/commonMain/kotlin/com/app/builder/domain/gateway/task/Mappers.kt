package com.app.builder.domain.gateway.task

import com.app.builder.data.database.TaskSchema
import com.app.builder.domain.Task

/**
 * Maps a [TaskSchema] entity to a [Task] domain model.
 *
 * @return The corresponding [Task] instance.
 */
fun TaskSchema.toTask(): Task = Task(
    uuid = uuid,
    modifiedAt = modifiedAt,
    deletedAt = deletedAt,
    title = title,
    description = description,
    state = state
)

/**
 * Maps a [Task] domain model to a [TaskSchema] entity.
 *
 * @return The corresponding [TaskSchema] instance.
 */
fun Task.toTaskSchema(): TaskSchema = TaskSchema(
    uuid = uuid,
    modifiedAt = modifiedAt,
    deletedAt = deletedAt,
    title = title,
    description = description,
    state = state
)
