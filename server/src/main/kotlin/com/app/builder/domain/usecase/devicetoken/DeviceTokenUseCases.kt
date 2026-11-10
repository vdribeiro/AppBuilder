package com.app.builder.domain.usecase.devicetoken

import kotlin.uuid.Uuid
import com.app.builder.domain.DeviceToken
import com.app.builder.domain.usecase.Outcome

/** Defines the business workflows for managing device push tokens. */
interface DeviceTokenUseCases {

    /**
     * Registers or updates a device's FCM token for a user.
     *
     * @param userUuid The owning user's UUID.
     * @param registration The device token registration form.
     * @return [Outcome.Success] when persisted, or [Outcome.Error] when the operation failed.
     */
    suspend fun registerToken(userUuid: Uuid, registration: DeviceToken): Outcome<Unit>

    /**
     * Retrieves all registered device tokens for a user.
     *
     * @param userUuid The target user's UUID.
     * @return [Outcome.Success] with the registered [DeviceToken]s of the user, or [Outcome.Error] when the operation failed.
     */
    suspend fun getTokens(userUuid: Uuid): Outcome<List<DeviceToken>>

    /**
     * Removes a device token.
     *
     * @param token The FCM registration token to remove.
     * @return [Outcome.Success] when a token was removed, [Outcome.NotFound] when none matched, or [Outcome.Error] when the operation failed.
     */
    suspend fun removeToken(token: String): Outcome<Unit>
}
