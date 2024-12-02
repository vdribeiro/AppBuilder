package com.app.builder.core.nfc

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import com.app.builder.core.telemetry.Telemetry

/** Provider to manage nfc tag reads. */
object TagProvider {
    private const val TAG = "TagProvider"

    /** Backing state for [tags]. */
    private val _tags: Channel<NfcTag> = Channel(capacity = Channel.BUFFERED)
    /** Observable flow of tag reads. */
    val tags: Flow<NfcTag> = _tags.receiveAsFlow()

    /** Discards any tag buffered but not yet collected. */
    fun reset() {
        var count = 0
        while (_tags.tryReceive().isSuccess) count++
        Telemetry.info(tag = TAG, message = "Discarded $count tags.")
    }

    /**
     * Sends a [tag] to the channel.
     *
     * @param tag The tag to handle.
     */
    internal fun sendTag(tag: NfcTag) {
        _tags.trySend(element = tag)
    }
}