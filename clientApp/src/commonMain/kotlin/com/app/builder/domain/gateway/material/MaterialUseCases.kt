package com.app.builder.domain.gateway.material

import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow
import com.app.builder.domain.Material

/** Defines the business workflows for materials. */
interface MaterialUseCases {

    /**
     * Observes the complete list of materials.
     *
     * @return [Flow] emitting the current list of [Material] objects.
     */
    fun observeMaterials(): Flow<List<Material>>

    /**
     * Observes a specific application material by its unique identifier.
     *
     * @param uuid The unique identifier of the material to observe.
     * @return [Flow] emitting the [Material] if found, or null if it does not exist.
     */
    fun observeMaterial(uuid: Uuid): Flow<Material?>

    /**
     * Inserts a new material or updates an existing one.
     *
     * @param material The [Material] to be inserted or updated.
     */
    suspend fun upsertMaterial(material: Material)
}
