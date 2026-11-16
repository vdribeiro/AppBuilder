package com.app.builder.ui.component.link

import com.app.builder.domain.Account
import com.app.builder.domain.EntityType
import com.app.builder.domain.Permission

/**
 * What an account can do with the links of the entity being shown.
 *
 * @property canRead Whether the links can be seen.
 * @property canEdit Whether the links can be added, changed and removed.
 */
data class LinkPermissions(
    val canRead: Boolean = false,
    val canEdit: Boolean = false,
)

/**
 * Works out what this account can do with the links to the [linked] entities: it can read them when the links are [enabled] and it can read both the [linked] entities and the [link] entities, and edit them when it can also write the [link] entities.
 *
 * @param enabled Whether the links are enabled.
 * @param linked The type of the linked entities.
 * @param link The type of the entities linking them.
 * @return The link permissions, none without an account.
 */
fun Account?.linkPermissions(enabled: Boolean, linked: EntityType, link: EntityType): LinkPermissions {
    if (this == null || !enabled || !hasPermission(entityType = linked, permission = Permission.READ)) return LinkPermissions()
    return LinkPermissions(
        canRead = hasPermission(entityType = link, permission = Permission.READ),
        canEdit = hasPermission(entityType = link, permission = Permission.WRITE)
    )
}
