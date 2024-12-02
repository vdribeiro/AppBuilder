package com.app.builder.domain.gateway.push

import com.app.builder.domain.PushPayload

/** Defines the business workflows for sending push payloads. */
interface PushUseCases {

    /**
     * Sends a [PushPayload].
     *
     * @param pushPayload The push payload to send.
     * @return `true` if the push was sent successfully, `false` otherwise.
     */
    suspend fun push(pushPayload: PushPayload): Boolean
}
