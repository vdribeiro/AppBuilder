package com.app.builder.domain.gateway.task

/** Selects which tasks are observed by their deletion. */
sealed interface TaskFilter {
    /** Every task, deleted or not. */
    data object All: TaskFilter
    /** Tasks that are not deleted. */
    data object Active: TaskFilter
}
