package com.app.builder.domain.usecase.config

import com.app.builder.core.config.ClientConfigs
import com.app.builder.core.config.ClientFlags
import com.app.builder.core.config.ServerConfigs
import com.app.builder.core.config.ServerFlags
import com.app.builder.domain.usecase.Outcome

/** Feature flags and configs use cases. */
interface ConfigUseCases {

    /** Restores persisted config records from disk. */
    suspend fun load()

    /**
     * Updates the client feature flags.
     *
     * @param flags The new client feature flags.
     * @return [Outcome.Success] when persisted, or [Outcome.Error] when the operation failed.
     */
    suspend fun updateClientFeatureFlags(flags: ClientFlags): Outcome<Unit>

    /**
     * Updates the client configs.
     *
     * @param configs The new client configs.
     * @return [Outcome.Success] when persisted, or [Outcome.Error] when the operation failed.
     */
    suspend fun updateClientConfigs(configs: ClientConfigs): Outcome<Unit>

    /**
     * Updates the server feature flags.
     *
     * @param flags The new server feature flags.
     * @return [Outcome.Success] when persisted, or [Outcome.Error] when the operation failed.
     */
    suspend fun updateServerFeatureFlags(flags: ServerFlags): Outcome<Unit>

    /**
     * Updates the server configs.
     *
     * @param configs The new server configs.
     * @return [Outcome.Success] when persisted, or [Outcome.Error] when the operation failed.
     */
    suspend fun updateServerConfigs(configs: ServerConfigs): Outcome<Unit>
}
