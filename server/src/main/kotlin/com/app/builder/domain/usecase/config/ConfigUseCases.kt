package com.app.builder.domain.usecase.config

import com.app.builder.core.config.ClientConfigs
import com.app.builder.core.config.ClientFlags
import com.app.builder.core.config.ServerConfigs
import com.app.builder.core.config.ServerFlags

/** Feature flags and configs use cases. */
interface ConfigUseCases {

    /** Restores persisted config records from disk. */
    suspend fun load()

    /**
     * Updates the client feature flags.
     *
     * @param flags The new client feature flags.
     * @return `true` if the new value was persisted successfully, `false` otherwise.
     */
    suspend fun updateClientFeatureFlags(flags: ClientFlags): Boolean

    /**
     * Updates the client configs.
     *
     * @param configs The new client configs.
     * @return `true` if the new value was persisted successfully, `false` otherwise.
     */
    suspend fun updateClientConfigs(configs: ClientConfigs): Boolean

    /**
     * Updates the server feature flags.
     *
     * @param flags The new server feature flags.
     * @return `true` if the new value was persisted successfully, `false` otherwise.
     */
    suspend fun updateServerFeatureFlags(flags: ServerFlags): Boolean

    /**
     * Updates the server configs.
     *
     * @param configs The new server configs.
     * @return `true` if the new value was persisted successfully, `false` otherwise.
     */
    suspend fun updateServerConfigs(configs: ServerConfigs): Boolean
}
