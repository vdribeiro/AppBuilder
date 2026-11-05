package com.app.builder.domain.gateway.task

import com.app.builder.data.database.TaskSchema
import com.app.builder.data.database.TaskUserAccessSchema
import com.app.builder.domain.Task
import com.app.builder.domain.TaskUserAccess

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

/**
 * Maps a [TaskUserAccessSchema] entity to a [TaskUserAccess] domain model.
 *
 * @return The corresponding [TaskUserAccess] instance.
 */
fun TaskUserAccessSchema.toTaskUserAccess(): TaskUserAccess = TaskUserAccess(
    uuid = uuid,
    modifiedAt = modifiedAt,
    deletedAt = deletedAt,
    taskUuid = taskUuid,
    userUuid = userUuid
)

/**
 * Maps a [TaskUserAccess] domain model to a [TaskUserAccessSchema] entity.
 *
 * @return The corresponding [TaskUserAccessSchema] instance.
 */
fun TaskUserAccess.toTaskUserAccessSchema(): TaskUserAccessSchema = TaskUserAccessSchema(
    uuid = uuid,
    modifiedAt = modifiedAt,
    deletedAt = deletedAt,
    taskUuid = taskUuid,
    userUuid = userUuid
)
