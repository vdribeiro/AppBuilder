package com.app.builder.domain

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import com.app.builder.test.TestCase

class PermissionTest: TestCase() {

    /** Verifies write grants read, read does not grant write, and an unlisted entity type grants nothing. */
    @Test
    fun grants() = runUnitTest {
        val permissions = mapOf(EntityType.TASK to Permission.WRITE, EntityType.USER to Permission.READ)
        assertTrue(actual = permissions.grants(entityType = EntityType.TASK, permission = Permission.READ))
        assertTrue(actual = permissions.grants(entityType = EntityType.TASK, permission = Permission.WRITE))
        assertTrue(actual = permissions.grants(entityType = EntityType.USER, permission = Permission.READ))
        assertFalse(actual = permissions.grants(entityType = EntityType.USER, permission = Permission.WRITE))
        assertFalse(actual = permissions.grants(entityType = EntityType.BUILDING, permission = Permission.READ))
    }

    /** Verifies the building equipment permission stands on its own, whatever the building and equipment permissions are. */
    @Test
    fun buildingEquipmentIsItsOwnPermission() = runUnitTest {
        val withoutLinks = mapOf(EntityType.BUILDING to Permission.WRITE, EntityType.EQUIPMENT to Permission.WRITE)
        assertFalse(actual = withoutLinks.grants(entityType = EntityType.BUILDING_EQUIPMENT, permission = Permission.READ))

        val linksOnly = mapOf(EntityType.BUILDING_EQUIPMENT to Permission.WRITE)
        assertTrue(actual = linksOnly.grants(entityType = EntityType.BUILDING_EQUIPMENT, permission = Permission.WRITE))
        assertFalse(actual = linksOnly.grants(entityType = EntityType.BUILDING, permission = Permission.READ))
    }
}
