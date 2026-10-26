package com.app.builder.domain.push

import kotlinx.coroutines.channels.Channel
import com.app.builder.domain.PushPayload

/**
 * A [FcmService] recording every topic and multicast send, so tests can assert exactly what was delivered and to which tokens.
 *
 * @property unregisteredTokens The tokens reported back as no longer registered whenever a multicast targets them.
 */
class FakeFcmService(private val unregisteredTokens: Set<String> = emptySet()): FcmService {

    /** Emits the payload of each [sendToBroadcastTopic] call. */
    val broadcasts = Channel<PushPayload>(capacity = Channel.UNLIMITED)

    /** Emits the token list of each [sendToTokens] call. */
    val multicasts = Channel<List<String>>(capacity = Channel.UNLIMITED)

    override suspend fun subscribeToBroadcastTopic(token: String): Boolean = true

    override suspend fun sendToBroadcastTopic(pushPayload: PushPayload): Boolean {
        broadcasts.trySend(element = pushPayload)
        return true
    }

    override suspend fun sendToTokens(tokens: List<String>, pushPayload: PushPayload): List<String> {
        multicasts.trySend(element = tokens)
        return tokens.filter { it in unregisteredTokens }
    }
}
