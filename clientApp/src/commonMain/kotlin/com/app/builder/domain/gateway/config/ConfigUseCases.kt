package com.app.builder.domain.gateway.config

import com.app.builder.core.config.ClientConfigs
import com.app.builder.core.config.ClientFlags
import com.app.builder.core.config.ServerConfigs
import com.app.builder.core.config.ServerFlags

/** Feature flags and configs use cases. */
interface ConfigUseCases {

    /**
     * Fetches the current server feature flags.
     *
     * @return The server feature flags, or `null` if the request fails.
     */
    suspend fun getServerFeatureFlags(): ServerFlags?

    /**
     * Fetches the current server configs.
     *
     * @return The server configs, or `null` if the request fails.
     */
    suspend fun getServerConfigs(): ServerConfigs?

    /**
     * Updates feature flags.
     *
     * @param flags The new feature flags.
     * @return The persisted value, or `null` if the request fails.
     */
    suspend fun updateFeatureFlags(flags: ClientFlags): ClientFlags?

    /**
     * Updates configs.
     *
     * @param configs The new configs.
     * @return The persisted value, or `null` if the request fails.
     */
    suspend fun updateConfigs(configs: ClientConfigs): ClientConfigs?

    /**
     * Updates the server feature flags.
     *
     * @param flags The new server feature flags.
     * @return The persisted value, or `null` if the request fails.
     */
    suspend fun updateServerFeatureFlags(flags: ServerFlags): ServerFlags?

    /**
     * Updates the server configs.
     *
     * @param configs The new server configs.
     * @return The persisted value, or `null` if the request fails.
     */
    suspend fun updateServerConfigs(configs: ServerConfigs): ServerConfigs?
}
