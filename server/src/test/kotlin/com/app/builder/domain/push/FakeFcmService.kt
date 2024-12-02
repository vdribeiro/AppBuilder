package com.app.builder.domain.push

import kotlinx.coroutines.channels.Channel
import com.app.builder.domain.PushPayload

/** A [FcmService] recording every multicast send, so tests can assert exactly which tokens were targeted. */
class FakeFcmService: FcmService {

    /** Emits the token list of each [sendToTokens] call. */
    val multicasts = Channel<List<String>>(capacity = Channel.UNLIMITED)

    override suspend fun subscribeToBroadcastTopic(token: String): Boolean = true

    override suspend fun sendToBroadcastTopic(pushPayload: PushPayload): Boolean = true

    override suspend fun sendToTokens(tokens: List<String>, pushPayload: PushPayload): List<String> {
        multicasts.trySend(element = tokens)
        return emptyList()
    }
}
