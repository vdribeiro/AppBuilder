package com.app.builder.domain.usecase

/** Outcome of a write that can be refused for reasons the caller reports differently. */
enum class WriteResult {
    /** The write was applied. */
    SUCCESS,
    /** The write was refused because its payload is inconsistent. */
    INVALID,
    /** The write was refused because the entity it updates doesn't exist. */
    NOT_FOUND,
    /** The write was refused because it clashes with an existing entity. */
    CONFLICT,
    /** The write failed. */
    ERROR,
}
