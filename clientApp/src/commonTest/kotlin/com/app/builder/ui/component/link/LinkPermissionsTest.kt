package com.app.builder.ui.component.link

import kotlin.test.Test
import kotlin.test.assertEquals
import com.app.builder.domain.Account
import com.app.builder.domain.EntityType
import com.app.builder.domain.Permission
import com.app.builder.test.TestCase

class LinkPermissionsTest: TestCase() {

    /**
     * Works out the building material link permissions of an account with the given [permissions].
     *
     * @param permissions The permissions of the account.
     * @param enabled Whether the links are enabled.
     * @return The link permissions.
     */
    private fun linkPermissions(permissions: Map<EntityType, Permission>, enabled: Boolean = true): LinkPermissions =
        Account(username = "user", permissions = permissions).linkPermissions(enabled = enabled, linked = EntityType.MATERIAL, link = EntityType.BUILDING_MATERIAL)

    /** Verifies that links can be read with read permission on the linked and the link entities, and edited with write permission on the link entities. */
    @Test
    fun readAndEdit() = runUnitTest {
        assertEquals(
            expected = LinkPermissions(canRead = true),
            actual = linkPermissions(permissions = mapOf(EntityType.MATERIAL to Permission.READ, EntityType.BUILDING_MATERIAL to Permission.READ))
        )
        assertEquals(
            expected = LinkPermissions(canRead = true, canEdit = true),
            actual = linkPermissions(permissions = mapOf(EntityType.MATERIAL to Permission.READ, EntityType.BUILDING_MATERIAL to Permission.WRITE))
        )
    }

    /** Verifies that there are no link permissions without an account, with the links disabled, or without read permission on the linked or the link entities. */
    @Test
    fun none() = runUnitTest {
        val all = mapOf(EntityType.MATERIAL to Permission.WRITE, EntityType.BUILDING_MATERIAL to Permission.WRITE)

        assertEquals(expected = LinkPermissions(), actual = null.linkPermissions(enabled = true, linked = EntityType.MATERIAL, link = EntityType.BUILDING_MATERIAL))
        assertEquals(expected = LinkPermissions(), actual = linkPermissions(permissions = all, enabled = false))
        assertEquals(expected = LinkPermissions(), actual = linkPermissions(permissions = all - EntityType.MATERIAL))
        assertEquals(expected = LinkPermissions(), actual = linkPermissions(permissions = all - EntityType.BUILDING_MATERIAL))
    }
}
