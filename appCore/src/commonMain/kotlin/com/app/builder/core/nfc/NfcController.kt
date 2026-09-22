package com.app.builder.core.nfc

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import com.app.builder.core.config.ClientFlags
import com.app.builder.core.telemetry.Telemetry

/** Platform-agnostic NFC reader/writer controller intended to be extended by platform-specific implementations. */
open class NfcController {

    /** Observable state of the NFC session. */
    sealed interface State {
        /** No active scan or write session. */
        data object Idle: State
        /** Actively scanning for a tag to read. */
        data object Scanning: State
        /** Actively waiting for a tag to write to. */
        data object Writing: State
    }

    /** If the nfc adapter is available on this device. */
    open val available: Boolean = false

    /** Backing state for [state]. */
    private val _state: MutableStateFlow<State> = MutableStateFlow(value = State.Idle)
    /** Observable state of the NFC session. */
    val state: StateFlow<State> = _state.asStateFlow()

    /** Stops any active session and releases nfc resources. */
    fun dispose() {
        stop()
        runCatching {
            platformDispose()
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Error disposing nfc", throwable = it)
        }
    }

    /** Stops the active scan or write session. Transitions to [State.Idle]. */
    fun stop() {
        runCatching {
            platformStop()
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Error stopping scan", throwable = it)
        }
        _state.update { State.Idle }
    }

    /** Starts scanning for a tag to read. Transitions to [State.Scanning]. */
    fun read() {
        runCatching {
            if (!ClientFlags.flags.nfc || !available) return@runCatching stop()
            stop()
            platformRead()
            _state.update { State.Scanning }
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Error starting read", throwable = it)
            stop()
        }
    }

    /**
     * Starts scanning for a tag to write [records] to. Transitions to [State.Writing].
     *
     * @param records The records to write to the tag.
     */
    fun write(records: List<NfcRecord>) {
        runCatching {
            if (!ClientFlags.flags.nfc || !available) return@runCatching stop()
            stop()
            platformWrite(records = records)
            _state.update { State.Writing }
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Error starting write", throwable = it)
            stop()
        }
    }

    /** Release nfc resources. */
    protected open fun platformDispose() {}

    /** Stops the platform scan or write session. */
    protected open fun platformStop() {}

    /** Starts the platform scan session. */
    protected open fun platformRead() {}

    /**
     * Starts the platform write session for [records].
     *
     * @param records The records to write to the tag.
     */
    protected open fun platformWrite(records: List<NfcRecord>) {}

    companion object {
        private const val TAG = "NfcController"

        /**
         * Creates a [NfcController].
         *
         * @return [NfcController] instance.
         */
        internal fun create(): NfcController = runCatching {
            createNfcController().also { Telemetry.info(tag = TAG, message = "NFC controller created") }
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to create NFC controller", throwable = it)
        }.getOrDefault(defaultValue = NfcController())
    }
}

/**
 * Creates a platform-specific [NfcController] instance.
 *
 * @return A new [NfcController] ready to receive commands.
 */
internal expect fun createNfcController(): NfcController
