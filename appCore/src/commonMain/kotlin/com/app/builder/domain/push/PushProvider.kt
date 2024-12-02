package com.app.builder.domain.push

import kotlin.uuid.Uuid
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.app.builder.core.config.ClientConfigs
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.domain.PushPayload
import com.app.builder.domain.deeplink.DeepLink
import com.app.builder.domain.push.PushProvider.handledPayloads
import com.app.builder.domain.push.PushProvider.pushPayloads

/** Provider to manage push payloads. */
object PushProvider {

    private const val TAG = "PushProvider"

    /** Token backing state. */
    private val _token: MutableStateFlow<String?> = MutableStateFlow(value = null)
    /** The current FCM token, or null when none has been registered. */
    internal val token: StateFlow<String?> = _token.asStateFlow()

    /** Backing state for [pushPayloads]. */
    private val _pushPayloads: Channel<PushPayload> = Channel(capacity = Channel.BUFFERED)
    /** Observable flow of push payloads. */
    val pushPayloads: Flow<PushPayload> = _pushPayloads.receiveAsFlow()

    /** Guards [handledPayloads] against concurrent access from different push channels. */
    private val handledPayloadsMutex = Mutex()

    /** The most recently handled payload UUIDs, oldest first. */
    private val handledPayloads = ArrayDeque<Uuid>()

    /** Clears the token, the payload history, and any payload buffered but not yet collected. */
    suspend fun reset() {
        _token.value = null
        handledPayloadsMutex.withLock { handledPayloads.clear() }
        var count = 0
        while (_pushPayloads.tryReceive().isSuccess) count++
        Telemetry.info(tag = TAG, message = "Discarded $count payloads.")
    }

    /**
     * Sets the FCM token.
     *
     * @param token The new token, or `null` to clear it.
     */
    internal fun setToken(token: String?) {
        _token.value = token
    }

    /**
     * Handles a received [PushPayload], ignoring duplicate deliveries.
     *
     * @param pushPayload The payload to handle.
     * @return true if the [pushPayload] was handled, false otherwise.
     */
    internal suspend fun handlePushPayload(pushPayload: PushPayload): Boolean {
        if (!mark(uuid = pushPayload.uuid)) {
            Telemetry.info(tag = TAG, message = "Ignoring duplicate push payload: ${pushPayload.uuid}")
            return false
        }
        return _pushPayloads.trySend(element = pushPayload).isSuccess
    }

    /**
     * Handles a notification click by dispatching the corresponding [DeepLink], if available.
     *
     * @param pushPayload The payload of the clicked notification.
     */
    internal fun handleNotificationClick(pushPayload: PushPayload) {
        pushPayload.toDeepLink()?.let { DeepLink.send(deepLink = it) }
    }

    /**
     * Records [uuid] as handled, unless it already was.
     *
     * @param uuid The payload UUID to record.
     * @return `true` the first time a given [uuid] is seen, `false` on every subsequent call.
     */
    private suspend fun mark(uuid: Uuid): Boolean = handledPayloadsMutex.withLock {
        if (handledPayloads.contains(element = uuid)) return@withLock false
        handledPayloads.addLast(element = uuid)
        if (handledPayloads.size > ClientConfigs.configs.maxHandledPayloads) handledPayloads.removeFirst()
        true
    }

    /**
     * Map a [PushPayload] to a [DeepLink].
     *
     * @return The converted [DeepLink].
     */
    private fun PushPayload.toDeepLink(): DeepLink? = when (this) {
        is PushPayload.EntityNotification -> DeepLink.Entity(
            entityUuid = entityUuid,
            entityType = entityType
        )

        is PushPayload.Notification,
        is PushPayload.Flags,
        is PushPayload.Configs,
        is PushPayload.Reset -> null
    }
}
