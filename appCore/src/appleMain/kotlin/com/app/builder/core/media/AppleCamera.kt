package com.app.builder.core.media

import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDeviceInput
import platform.AVFoundation.AVCaptureDevicePositionBack
import platform.AVFoundation.AVCaptureDevicePositionFront
import platform.AVFoundation.AVCaptureDeviceTypeBuiltInWideAngleCamera
import platform.AVFoundation.AVCaptureFileOutput
import platform.AVFoundation.AVCaptureFileOutputRecordingDelegateProtocol
import platform.AVFoundation.AVCaptureInput
import platform.AVFoundation.AVCaptureMovieFileOutput
import platform.AVFoundation.AVCapturePhoto
import platform.AVFoundation.AVCapturePhotoCaptureDelegateProtocol
import platform.AVFoundation.AVCapturePhotoOutput
import platform.AVFoundation.AVCapturePhotoSettings
import platform.AVFoundation.AVCaptureSession
import platform.AVFoundation.AVCaptureSessionPresetHigh
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.defaultDeviceWithDeviceType
import platform.AVFoundation.fileDataRepresentation
import platform.Foundation.NSError
import platform.Foundation.NSURL
import platform.Foundation.writeToFile
import platform.darwin.NSObject
import com.app.builder.core.locale.epoch
import com.app.builder.core.platform.appCachePath
import com.app.builder.core.telemetry.Telemetry

/** Apple [Camera] implementation backed by AVFoundation. */
internal class AppleCamera: Camera() {

    /** The AVFoundation capture session. */
    private var session: AVCaptureSession? = null
    /** Handles still-image capture. */
    private val photoOutput: AVCapturePhotoOutput = AVCapturePhotoOutput()
    /** Handles video-to-file recording. */
    private val movieOutput: AVCaptureMovieFileOutput = AVCaptureMovieFileOutput()

    override val available: Boolean = runCatching {
        AVCaptureDevice.defaultDeviceWithMediaType(mediaType = AVMediaTypeVideo) != null
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable to check camera availability", throwable = it)
    }.getOrDefault(defaultValue = false)

    /**
     * Initializes the capture session.
     *
     * @return The initialized session or null if an error occurred.
     */
    fun init(): AVCaptureSession? = runCatching {
        session?.let { return it }

        val session = AVCaptureSession().apply {
            sessionPreset = AVCaptureSessionPresetHigh
            if (canAddOutput(output = photoOutput)) addOutput(output = photoOutput)
            if (canAddOutput(output = movieOutput)) addOutput(output = movieOutput)
        }
        this.session = session
        attachInput(session = session)

        session
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable to initialize AVCaptureSession", throwable = it)
    }.getOrNull()

    override fun platformDispose() {
        super.platformDispose()
        session = null
    }

    /** Resolves the wide-angle video device for the current [facing] direction, falling back to the default device, and adds it as an input to [session]. */
    private fun attachInput(session: AVCaptureSession) = runCatching {
        val position = if (facing.value == Facing.BACK) AVCaptureDevicePositionBack else AVCaptureDevicePositionFront
        val device = AVCaptureDevice.defaultDeviceWithDeviceType(
            deviceType = AVCaptureDeviceTypeBuiltInWideAngleCamera,
            mediaType = AVMediaTypeVideo,
            position = position,
        ) ?: AVCaptureDevice.defaultDeviceWithMediaType(mediaType = AVMediaTypeVideo) ?: error(message = "Unable to resolve video device")
        val input = AVCaptureDeviceInput(device = device, error = null)
        if (session.canAddInput(input = input)) session.addInput(input = input)
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable to attach default video device", throwable = it)
    }

    override fun hasPermission(): Boolean {
        super.hasPermission()
        return runCatching {
            AVCaptureDevice.authorizationStatusForMediaType(mediaType = AVMediaTypeVideo) == AVAuthorizationStatusAuthorized
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to check camera permission", throwable = it)
        }.getOrDefault(defaultValue = false)
    }

    /** Starts the capture session if it is not already running. */
    override fun platformStartPreview() {
        super.platformStartPreview()
        val session = this@AppleCamera.session ?: error(message = "No session")
        if (!session.isRunning()) session.startRunning()
    }

    /** Stops any active recording then stops the capture session. Guards against calling stop on an already-stopped session. */
    override fun platformStopPreview() {
        super.platformStopPreview()
        if (movieOutput.isRecording()) movieOutput.stopRecording()
        val session = this@AppleCamera.session ?: error(message = "No session")
        if (session.isRunning()) session.stopRunning()
    }

    /**
     * Captures a still image to a timestamped `.jpg` file in [appCachePath].
     *
     * @param onResult Invoked with the absolute path once written, or null on failure.
     */
    override fun platformCapturePhoto(onResult: (String?) -> Unit) {
        super.platformCapturePhoto(onResult = onResult)
        val path = "$appCachePath/photo_${epoch()}.jpg"
        photoOutput.capturePhotoWithSettings(
            settings = AVCapturePhotoSettings.photoSettings(),
            delegate = PhotoDelegate(path = path, onSaved = onResult),
        )
    }

    /**
     * Starts recording video to a timestamped `.mov` file in [appCachePath].
     *
     * @param onResult Invoked with the absolute path once the recording is finalized without error.
     */
    override fun platformStartRecording(onResult: (String) -> Unit) {
        super.platformStartRecording(onResult = onResult)
        val url = NSURL.fileURLWithPath(path = "$appCachePath/video_${epoch()}.mov")
        movieOutput.startRecordingToOutputFileURL(
            outputFileURL = url,
            recordingDelegate = MovieDelegate(onFinish = onResult, onError = { stopPreview() }),
        )
    }

    /** Stops the active movie recording if one is in progress. */
    override fun platformStopRecording() {
        super.platformStopRecording()
        if (movieOutput.isRecording()) movieOutput.stopRecording()
    }

    /**
     * Swaps the active capture input inside a single atomic configuration block so the session does not interrupt mid-frame.
     * Removes all current inputs then calls [attachInput] to re-attach using the new [facing] direction.
     */
    override fun platformToggleFacing() {
        super.platformToggleFacing()
        val session = this@AppleCamera.session ?: return
        session.beginConfiguration()
        session.inputs.toList().forEach { session.removeInput(input = it as AVCaptureInput) }
        attachInput(session = session)
        session.commitConfiguration()
    }

    /**
     * AVFoundation delegate that receives the processed photo data and writes it to [path].
     * [onSaved] is always invoked exactly once: with [path] after the file has been written atomically, or with null if [error] was set or the photo carried no data.
     *
     * @property path The file path the captured JPEG data is written to.
     * @property onSaved Invoked with [path], or null on failure, once processing has finished.
     */
    private class PhotoDelegate(
        private val path: String,
        private val onSaved: (String?) -> Unit,
    ): NSObject(), AVCapturePhotoCaptureDelegateProtocol {
        override fun captureOutput(output: AVCapturePhotoOutput, didFinishProcessingPhoto: AVCapturePhoto, error: NSError?) {
            val data = didFinishProcessingPhoto.fileDataRepresentation()
            if (error != null || data == null) {
                onSaved(null)
                return
            }
            data.writeToFile(path = path, atomically = true)
            onSaved(path)
        }
    }

    /**
     * AVFoundation delegate that fires after a movie recording has been finalized.
     * [onFinish] is invoked with the output file path only when [error] is null, indicating a clean finalization.
     * [onError] is invoked instead when the recording terminated due to an error (e.g. the camera permission was revoked mid-recording).
     *
     * @property onFinish Invoked with the absolute file path once a recording finalizes without error.
     * @property onError Invoked when the recording finalized with an error.
     */
    private class MovieDelegate(
        private val onFinish: (String) -> Unit,
        private val onError: () -> Unit,
    ): NSObject(), AVCaptureFileOutputRecordingDelegateProtocol {
        override fun captureOutput(output: AVCaptureFileOutput, didFinishRecordingToOutputFileAtURL: NSURL, fromConnections: List<*>, error: NSError?) {
            if (error != null) {
                Telemetry.error(tag = TAG, message = "Recording finalized with error", throwable = Throwable(message = error.localizedDescription))
                onError()
                return
            }
            onFinish(didFinishRecordingToOutputFileAtURL.path ?: return)
        }
    }

    companion object {
        private const val TAG = "AppleCamera"
    }
}

internal actual fun createCamera(): Camera = AppleCamera()
