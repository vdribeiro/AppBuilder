package com.app.builder.domain.push

import com.app.builder.domain.PushPayload

/** A no-op [FcmService]. */
object NoOpFcmService: FcmService {
    override suspend fun subscribeToBroadcastTopic(token: String): Boolean = true
    override suspend fun sendToBroadcastTopic(pushPayload: PushPayload): Boolean = true
    override suspend fun sendToTokens(tokens: List<String>, pushPayload: PushPayload): List<String> = emptyList()
}
