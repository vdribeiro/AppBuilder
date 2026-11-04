package com.app.builder.domain.gateway.status

import kotlinx.coroutines.flow.Flow
import com.app.builder.core.config.ClientConfigs

/** Defines the business workflows for following the application status. */
interface StatusUseCases {

    /**
     * Observes whether the background jobs are keeping up.
     * The client counts as synced while the scheduler or networking is turned off, since the work is then not meant to be uploaded.
     *
     * @return A flow emitting `false` while a job has been pending or running for longer than [ClientConfigs.schedulerSyncTolerance], `true` otherwise.
     */
    fun observeSynced(): Flow<Boolean>

    /**
     * Observes whether the device clock can be trusted.
     *
     * @return A flow emitting `true` while the device clock can be trusted, `false` otherwise.
     */
    fun observeClockTrusted(): Flow<Boolean>
}
