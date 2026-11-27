package com.app.builder.domain.gateway.timepunch

import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow
import com.app.builder.domain.TimePunch

/** Defines the business workflows for timePunches. */
interface TimePunchUseCases {

    /**
     * Observes the list of timePunches.
     *
     * @return [Flow] emitting the current list of [TimePunch] objects.
     */
    fun observeTimePunches(): Flow<List<TimePunch>>

    /**
     * Observes a specific time punch by its unique identifier.
     *
     * @param uuid The unique identifier of the time punch to observe.
     * @return [Flow] emitting the [TimePunch] if found, or null if it does not exist.
     */
    fun observeTimePunch(uuid: Uuid): Flow<TimePunch?>

    /**
     * Inserts a new time punch or updates an existing one.
     *
     * @param timePunch The [TimePunch] to be inserted or updated.
     */
    suspend fun upsertTimePunch(timePunch: TimePunch)
}
