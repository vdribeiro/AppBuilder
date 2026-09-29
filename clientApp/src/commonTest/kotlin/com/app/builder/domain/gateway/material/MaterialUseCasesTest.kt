package com.app.builder.domain.gateway.material

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.firstOrNull
import com.app.builder.Dependency.getUserDependency
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class MaterialUseCasesTest: TestCase() {

    /** Verifies that upserting a material makes it observable both individually and in the material list. */
    @Test
    fun crudMaterial() = runUnitTest {
        val materialUseCases = dependency.get()
            .getUserDependency(user = FakeData.adminUser)
            .useCases
            .materialUseCases

        assertTrue(actual = materialUseCases.observeMaterials().firstOrNull().orEmpty().isEmpty())
        materialUseCases.upsertMaterial(material = FakeData.material)
        assertEquals(expected = listOf(element = FakeData.material), actual = materialUseCases.observeMaterials().firstOrNull().orEmpty())
        assertEquals(expected = FakeData.material, actual = materialUseCases.observeMaterial(uuid = FakeData.material.uuid).firstOrNull())
    }
}
