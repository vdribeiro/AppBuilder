package com.app.builder.core.platform

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import com.app.builder.data.serializer.decode
import com.app.builder.domain.usecase.seed.Seed
import com.app.builder.test.TestCase

class ResourceTest: TestCase() {

    /** Verifies that reading an existing resource returns its text. */
    @Test
    fun readResource() = runServerTest {
        assertTrue(actual = readResource(path = "static/translations.json").orEmpty().isNotBlank())
    }

    /** Verifies that every key of the translations resource starts with the name of its group, belongs to one group only and has a value in every language. */
    @Test
    fun translationsResource() = runServerTest {
        val groups = assertNotNull(
            actual = decode<Map<String, Map<String, Map<String, String>>>>(value = readResource(path = "static/translations.json").orEmpty())
        )
        val languages = groups.values.flatMap { keys -> keys.values.flatMap { it.keys } }.toSet()
        assertTrue(actual = languages.isNotEmpty())

        groups.forEach { (group, keys) ->
            keys.forEach { (key, values) ->
                assertTrue(actual = key.startsWith(prefix = "${group}_"), message = key)
                assertEquals(expected = languages, actual = values.keys, message = key)
            }
        }
        val keys = groups.values.flatMap { it.keys }
        assertEquals(expected = keys.distinct(), actual = keys)
    }

    /** Verifies that the seed resource holds entities of every kind, registers each account for its own user, and links stored entities only. */
    @Test
    fun seedResource() = runServerTest {
        val seed = assertNotNull(actual = decode<Seed>(value = readResource(path = "static/seed.json").orEmpty()))
        val users = (seed.users + seed.registrations.map { it.user }).map { it.uuid }
        val buildings = seed.buildings.map { it.uuid }
        val equipments = seed.equipments.map { it.uuid }
        val materials = seed.materials.map { it.uuid }

        listOf(
            seed.users, seed.registrations, seed.tasks, seed.equipments, seed.buildings, seed.materials,
            seed.buildingEquipments, seed.buildingMaterials, seed.buildingUsers, seed.equipmentUsers, seed.registries
        ).forEach { assertTrue(actual = it.isNotEmpty()) }
        listOf(
            users, buildings, equipments, materials,
            seed.tasks.map { it.uuid },
            seed.buildingEquipments.map { it.uuid },
            seed.buildingMaterials.map { it.uuid },
            seed.buildingUsers.map { it.uuid },
            seed.equipmentUsers.map { it.uuid },
            seed.registries.map { it.requestUuid },
            seed.registrations.map { it.account.username }
        ).forEach { assertEquals(expected = it.distinct(), actual = it) }

        seed.registrations.forEach { assertEquals(expected = it.user.uuid, actual = it.account.userUuid, message = it.account.username) }
        seed.buildingEquipments.forEach { assertTrue(actual = it.buildingUuid in buildings && it.equipmentUuid in equipments, message = it.toString()) }
        seed.buildingMaterials.forEach { assertTrue(actual = it.buildingUuid in buildings && it.materialUuid in materials && it.quantity >= 1, message = it.toString()) }
        seed.buildingUsers.forEach { assertTrue(actual = it.buildingUuid in buildings && it.userUuid in users, message = it.toString()) }
        seed.equipmentUsers.forEach { assertTrue(actual = it.equipmentUuid in equipments && it.userUuid in users, message = it.toString()) }
    }

    /** Verifies that reading a missing resource returns null. */
    @Test
    fun readMissingResource() = runServerTest {
        assertNull(actual = readResource(path = "static/missing.json"))
    }
}
