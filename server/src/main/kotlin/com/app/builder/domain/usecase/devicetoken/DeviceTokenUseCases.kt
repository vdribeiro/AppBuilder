package com.app.builder.domain.usecase.devicetoken

import kotlin.uuid.Uuid
import com.app.builder.domain.DeviceToken

/** Defines the business workflows for managing device push tokens. */
interface DeviceTokenUseCases {

    /**
     * Registers or updates a device's FCM token for a user.
     *
     * @param userUuid The owning user's UUID.
     * @param registration The device token registration form.
     * @return `true` if the token was persisted successfully, `false` otherwise.
     */
    suspend fun registerToken(userUuid: Uuid, registration: DeviceToken): Boolean

    /**
     * Retrieves all registered device tokens for a user.
     *
     * @param userUuid The target user's UUID.
     * @return The list of registered [DeviceToken]s for the user.
     */
    suspend fun getTokens(userUuid: Uuid): List<DeviceToken>

    /**
     * Removes a device token.
     *
     * @param token The FCM registration token to remove.
     * @return `true` if a token was removed, `false` otherwise.
     */
    suspend fun removeToken(token: String): Boolean
}
