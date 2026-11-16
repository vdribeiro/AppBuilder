package com.app.builder.domain.gateway.material

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.firstOrNull
import com.app.builder.Dependency.getUserDependency
import com.app.builder.core.locale.now
import com.app.builder.core.security.uuid
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class MaterialUseCasesTest: TestCase() {

    /** Verifies that upserting a material makes it observable both individually and in the material list. */
    @Test
    fun crudMaterial() = runUnitTest {
        val materialUseCases = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
            .useCases
            .materialUseCases

        assertTrue(actual = materialUseCases.observeMaterials().firstOrNull().orEmpty().isEmpty())
        materialUseCases.upsertMaterial(material = FakeData.material)
        assertEquals(expected = listOf(element = FakeData.material), actual = materialUseCases.observeMaterials().firstOrNull().orEmpty())
        assertEquals(expected = FakeData.material, actual = materialUseCases.observeMaterial(uuid = FakeData.material.uuid).firstOrNull())
    }

    /** Verifies that the material list includes the deleted materials. */
    @Test
    fun filterMaterials() = runUnitTest {
        val materialUseCases = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
            .useCases
            .materialUseCases
        val deletedMaterial = FakeData.material.copy(uuid = uuid(), deletedAt = now())

        materialUseCases.upsertMaterial(material = FakeData.material)
        materialUseCases.upsertMaterial(material = deletedMaterial)

        assertEquals(expected = setOf(FakeData.material, deletedMaterial), actual = materialUseCases.observeMaterials().firstOrNull().orEmpty().toSet())
    }
}
