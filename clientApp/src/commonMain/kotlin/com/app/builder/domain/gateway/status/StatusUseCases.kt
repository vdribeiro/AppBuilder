package com.app.builder.domain.gateway.status

import kotlinx.coroutines.flow.Flow

/** Defines the business workflows for following the application status. */
interface StatusUseCases {

    /**
     * Observes whether every background job has been executed.
     *
     * @return A flow emitting `true` while no background job is pending or running, `false` otherwise.
     */
    fun observeSynced(): Flow<Boolean>

    /**
     * Observes whether the device clock can be trusted.
     *
     * @return A flow emitting `true` while the device clock can be trusted, `false` otherwise.
     */
    fun observeClockTrusted(): Flow<Boolean>
}
