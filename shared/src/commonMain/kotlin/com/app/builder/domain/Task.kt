package com.app.builder.domain

import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable
import com.app.builder.core.locale.now
import com.app.builder.core.security.uuid

/**
 * Task entity.
 *
 * @property uuid The unique, immutable identifier for this task.
 * @property modifiedAt The timestamp indicating when this task was last updated.
 * @property deletedAt The timestamp indicating when this task was last deleted.
 * @property title The title of the task.
 * @property description The descriptive content detailing the task.
 * @property state The task state.
 */
@Serializable
data class Task(
    val uuid: Uuid = uuid(),
    val modifiedAt: Instant = now(),
    val deletedAt: Instant? = null,
    val title: String = "",
    val description: String = "",
    val state: State = State.TODO
) {
    /**
     * Task state.
     *
     * @property translationKey The [Translation.key] used to look up the localized label for this field.
     */
    @Serializable
    enum class State(val translationKey: String) {
        TODO(translationKey = "task_state_todo"),
        COMPLETE(translationKey = "task_state_complete")
    }

    /**
     * Enumerates the [Task] fields that map to a localized display label.
     *
     * @property translationKey The [Translation.key] used to look up the localized label for this field.
     */
    @Serializable
    enum class Property(val translationKey: String) {
        MODIFIED_AT(translationKey = "task_modified_at"),
        DELETED_AT(translationKey = "task_deleted_at"),
        TITLE(translationKey = "task_title"),
        DESCRIPTION(translationKey = "task_description"),
        STATE(translationKey = "task_state")
    }
}