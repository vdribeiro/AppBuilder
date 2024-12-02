package com.app.builder.domain.deeplink

import kotlin.uuid.Uuid
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.domain.EntityType
import com.app.builder.domain.deeplink.DeepLink.Companion.deepLinks

/** Deep links. */
@Serializable
sealed interface DeepLink {

    @Serializable
    @SerialName(value = "entity")
    data class Entity(
        val entityUuid: Uuid?,
        val entityType: EntityType
    ): DeepLink

    companion object {
        private const val TAG = "DeepLink"

        /** Backing state for [deepLinks]. */
        private val _deepLinks: Channel<DeepLink> = Channel(capacity = Channel.CONFLATED)
        /** Observable flow of deep links. */
        val deepLinks: Flow<DeepLink> = _deepLinks.receiveAsFlow()

        /**
         * Sends a [deepLink] on [deepLinks].
         *
         * @param deepLink to send.
         */
        fun send(deepLink: DeepLink) {
            _deepLinks.trySend(element = deepLink)
        }

        /** Discards any deep link buffered but not yet collected. */
        fun reset() {
            var count = 0
            while (_deepLinks.tryReceive().isSuccess) count++
            Telemetry.info(tag = TAG, message = "Discarded $count deep links.")
        }
    }
}