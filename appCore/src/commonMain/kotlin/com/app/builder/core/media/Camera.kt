package com.app.builder.core.media

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import com.app.builder.core.config.ClientFlags
import com.app.builder.core.telemetry.Telemetry

/** Platform-agnostic camera controller intended to be extended by platform-specific implementations. */
open class Camera {

    /** Lens direction for the capture session. */
    enum class Facing {
        /** Front-facing camera. */
        FRONT,
        /** Rear-facing camera. */
        BACK,
    }

    /** Observable state of the camera session. */
    sealed interface State {
        /** No active capture session. */
        data object Idle: State
        /** Capture session is running. */
        data object Previewing: State
        /** Video recording is in progress. */
        data object Recording: State
    }

    /** If the camera is available on this device. */
    open val available: Boolean = false

    /** Backing state for [state]. */
    private val _state: MutableStateFlow<State> = MutableStateFlow(value = State.Idle)
    /** Observable state of the camera. */
    val state: StateFlow<State> = _state.asStateFlow()

    /** Backing state for [facing]. */
    private val _facing: MutableStateFlow<Facing> = MutableStateFlow(value = Facing.BACK)
    /** The active lens direction used when binding the capture session. */
    val facing: StateFlow<Facing> = _facing.asStateFlow()

    /**
     * Returns if the camera is permitted on this device.
     *
     * @return true if permitted, false otherwise.
     */
    open fun hasPermission(): Boolean = false

    /** Release camera resources. */
    fun dispose() {
        stopPreview()
        runCatching {
            platformDispose()
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Error disposing camera", throwable = it)
        }
    }

    /** Starts the platform capture session and attaches the surface. */
    fun startPreview() {
        runCatching {
            if (!ClientFlags.flags.camera || !available || !hasPermission()) return@runCatching stopPreview()
            platformStopRecording()
            platformStopPreview()
            platformStartPreview()
            _state.update { State.Previewing }
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Error starting preview", throwable = it)
            stopPreview()
        }
    }

    /** Stops the platform capture session and releases the surface. */
    fun stopPreview() {
        runCatching {
            platformStopRecording()
            platformStopPreview()
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Error stopping preview", throwable = it)
        }
        _state.update { State.Idle }
    }

    /**
     * Triggers a still image capture.
     *
     * @param onResult Invoked with the absolute file path or data URL of the captured photo, or null if the capture failed.
     */
    fun capturePhoto(onResult: (String?) -> Unit) {
        runCatching {
            if (!ClientFlags.flags.camera || !available || !hasPermission()) {
                stopPreview()
                return@runCatching onResult(null)
            }
            if (state.value != State.Previewing) return@runCatching onResult(null)
            platformCapturePhoto(onResult = onResult)
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Error capturing photo", throwable = it)
            onResult(null)
        }
    }

    /**
     * Begins writing video frames to a file.
     *
     * @param onResult Invoked with the absolute file path or data URL once the platform finishes writing the video, after [stopRecording] is triggered.
     */
    fun startRecording(onResult: (String) -> Unit) {
        runCatching {
            if (!ClientFlags.flags.camera || !available || !hasPermission()) return@runCatching stopPreview()
            if (state.value != State.Previewing) return@runCatching
            platformStopRecording()
            platformStartRecording(onResult = onResult)
            _state.update { State.Recording }
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Error starting recording", throwable = it)
        }
    }

    /** Finalizes the video file. The result must reach whichever `onResult` callback was passed to [startRecording]. */
    fun stopRecording() {
        runCatching {
            if (!ClientFlags.flags.camera || !available || !hasPermission()) return@runCatching stopPreview()
            if (state.value != State.Recording) return@runCatching
            platformStopRecording()
            _state.update { State.Previewing }
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Error stopping recording", throwable = it)
        }
    }

    /** Called after [facing] has been updated so the platform can rebind the capture session to the new lens. The current [State] is preserved across the switch. */
    fun toggleFacing() {
        runCatching {
            if (!ClientFlags.flags.camera || !available || !hasPermission()) return@runCatching stopPreview()
            if (state.value != State.Previewing) return@runCatching
            _facing.update {
                when (it) {
                    Facing.FRONT -> Facing.BACK
                    Facing.BACK -> Facing.FRONT
                }
            }
            platformToggleFacing()
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Error toggling facing", throwable = it)
        }
    }

    /** Release camera resources. */
    protected open fun platformDispose() {}

    /** Starts the platform capture session and attaches the surface. */
    protected open fun platformStartPreview() {}

    /** Stops the platform capture session and releases the surface. */
    protected open fun platformStopPreview() {}

    /**
     * Triggers a still image capture.
     *
     * @param onResult Invoked with the absolute file path or data URL of the captured photo, or null if the capture failed.
     */
    protected open fun platformCapturePhoto(onResult: (String?) -> Unit) {
        onResult(null)
    }

    /**
     * Begins writing video frames to a file.
     *
     * @param onResult Invoked with the absolute file path or data URL once the platform finishes writing the video, after [platformStopRecording] is triggered.
     */
    protected open fun platformStartRecording(onResult: (String) -> Unit) {}

    /** Finalizes the video file. The result must reach whichever `onResult` callback was passed to [platformStartRecording]. */
    protected open fun platformStopRecording() {}

    /** Called after [facing] has been updated so the platform can rebind the capture session to the new lens. The current [State] is preserved across the switch. */
    protected open fun platformToggleFacing() {}

    companion object {
        private const val TAG = "Camera"

        /**
         * Creates a [Camera].
         *
         * @return [Camera] instance.
         */
        internal fun create(): Camera = runCatching {
            createCamera().also { Telemetry.info(tag = TAG, message = "Camera created") }
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to create the Camera and returning a no-op camera", throwable = it)
        }.getOrDefault(defaultValue = Camera())
    }
}

/**
 * Creates a platform-specific [Camera] instance.
 *
 * @return A new [Camera] ready to receive commands.
 */
internal expect fun createCamera(): Camera
