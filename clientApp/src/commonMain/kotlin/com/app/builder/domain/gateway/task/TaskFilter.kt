package com.app.builder.domain.gateway.task

/** Selects which tasks are observed by their deletion. */
sealed interface TaskFilter {
    /** Tasks that are not deleted. */
    data object Active: TaskFilter
    /** Tasks that are deleted. */
    data object Deleted: TaskFilter
}
