package com.app.builder.domain

import kotlinx.serialization.Serializable

/** Permission types. */
@Serializable
enum class Permission {
    WRITE,
    READ
}
