package com.app.builder.core.media

import java.io.File
import android.Manifest
import android.content.pm.PackageManager
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.app.builder.applicationContext
import com.app.builder.core.platform.appCachePath
import com.app.builder.core.telemetry.Telemetry

/** Android [Camera] implementation backed by CameraX. */
internal class AndroidCamera: Camera() {

    /** CameraX binder that ties use cases to the Android lifecycle. */
    private var processCameraProvider: ProcessCameraProvider? = null
    /** Lifecycle owner used to bind CameraX use cases. */
    private var lifecycleOwner: LifecycleOwner? = null
    /** Surface that receives decoded camera frames for the live preview. */
    private var surfaceProvider: Preview.SurfaceProvider? = null
    /** Video recorder configured to use the highest quality available on the device. */
    private var recorder: Recorder? = null
    /** Video capture use case wired to [recorder]. Created once and reused across preview sessions so the recorder state is preserved when switching cameras. */
    private var videoCapture: VideoCapture<Recorder>? = null
    /** CameraX still-image use case. */
    private var imageCapture: ImageCapture? = null
    /** Handle to the ongoing video recording, or null when not recording. */
    private var activeRecording: Recording? = null
    /** Preview surface. */
    private var previewView: PreviewView? = null

    override val available: Boolean = runCatching {
        applicationContext.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable to check camera availability", throwable = it)
    }.getOrDefault(defaultValue = false)

    /**
     * Initializes the CameraX framework.
     *
     * @param lifecycleOwner The host lifecycle owner.
     * @return The [PreviewView], or null if an error occurred.
     */
    fun init(lifecycleOwner: LifecycleOwner): PreviewView? = runCatching {
        previewView?.let { return it }

        val previewView = PreviewView(applicationContext).apply { implementationMode = PreviewView.ImplementationMode.COMPATIBLE }
        val executor = ContextCompat.getMainExecutor(applicationContext)
        val future = ProcessCameraProvider.getInstance(context = applicationContext)
        future.addListener({ runCatching { processCameraProvider = future.get() }.onFailure { Telemetry.error(tag = TAG, message = "Unable to resolve the camera provider", throwable = it) } }, executor)

        this.lifecycleOwner = lifecycleOwner
        this.previewView = previewView
        this.surfaceProvider = previewView.surfaceProvider
        this.recorder = Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
            .build().also { videoCapture = VideoCapture.withOutput(it) }

        previewView
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable to initialize CameraX", throwable = it)
    }.getOrNull()

    /** Releases the CameraX framework to prevent the camera from holding a stale reference to destroyed references. */
    override fun platformDispose() {
        super.platformDispose()
        processCameraProvider = null
        lifecycleOwner = null
        surfaceProvider = null
        recorder = null
        videoCapture = null
        imageCapture = null
        activeRecording = null
        previewView = null
    }

    override fun hasPermission(): Boolean {
        super.hasPermission()
        return runCatching {
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to check camera permission", throwable = it)
        }.getOrDefault(defaultValue = false)
    }

    /**
     * Binds the [Preview], [ImageCapture], and [VideoCapture] to the lifecycle.
     * Any previously bound use cases are released first via [ProcessCameraProvider.unbindAll].
     * The camera lens is selected from the current [facing] value.
     */
    override fun platformStartPreview() {
        super.platformStartPreview()

        val cameraProvider = processCameraProvider ?: error(message = "No camera provider")
        val lifecycleOwner = this@AndroidCamera.lifecycleOwner ?: error(message = "No lifecycle owner")
        val surfaceProvider = this@AndroidCamera.surfaceProvider ?: error(message = "No surface provider")
        val videoCapture = this@AndroidCamera.videoCapture ?: error(message = "No video capture")

        val selector = if (facing.value == Facing.BACK) CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA
        val preview = Preview.Builder().build().apply { this.surfaceProvider = surfaceProvider }
        val imageCapture = ImageCapture.Builder().build().also { this@AndroidCamera.imageCapture = it }

        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            selector,
            preview,
            imageCapture,
            videoCapture
        )
    }

    /** Stops any active recording and unbinds all CameraX use cases from the lifecycle, releasing the camera hardware. */
    override fun platformStopPreview() {
        super.platformStopPreview()
        activeRecording?.stop()
        activeRecording = null
        processCameraProvider?.unbindAll()
    }

    /**
     * Captures a still image and writes it to a timestamped `.jpg` file in the application cache directory.
     *
     * @param onResult Invoked with the absolute path on success, or null on failure.
     */
    override fun platformCapturePhoto(onResult: (String?) -> Unit) {
        super.platformCapturePhoto(onResult = onResult)
        val capture = imageCapture ?: return onResult(null)
        val file = File(appCachePath, "photo_${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()
        val executor = ContextCompat.getMainExecutor(applicationContext)
        capture.takePicture(outputOptions, executor, object: ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                onResult(file.absolutePath)
            }

            override fun onError(exception: ImageCaptureException) {
                Telemetry.error(tag = TAG, message = "Unable to capture photo", throwable = exception)
                onResult(null)
            }
        })
    }

    /**
     * Begins recording video to a timestamped `.mp4` file in the application cache directory.
     *
     * @param onResult Invoked with the absolute path once the recording is finalized without error.
     */
    override fun platformStartRecording(onResult: (String) -> Unit) {
        super.platformStartRecording(onResult = onResult)
        val recorder = this@AndroidCamera.recorder ?: error(message = "No recorder")
        val file = File(appCachePath, "video_${System.currentTimeMillis()}.mp4")
        val outputOptions = FileOutputOptions.Builder(file).build()
        val executor = ContextCompat.getMainExecutor(applicationContext)
        activeRecording = recorder.prepareRecording(applicationContext, outputOptions)
            .start(executor) { event ->
                if (event is VideoRecordEvent.Finalize) {
                    when {
                        event.hasError() -> {
                            Telemetry.error(tag = TAG, message = "Recording finalized with error", throwable = Throwable(message = "Error code ${event.error}"))
                            stopPreview()
                        }

                        else -> onResult(file.absolutePath)
                    }
                }
            }
    }

    /** Stops the active recording and clears the [activeRecording] handle. */
    override fun platformStopRecording() {
        super.platformStopRecording()
        activeRecording?.stop()
        activeRecording = null
    }

    /** Responds to a lens direction change by stopping the current preview and immediately restarting it so [platformStartPreview] rebinds with the updated [facing] value. */
    override fun platformToggleFacing() {
        super.platformToggleFacing()
        platformStopPreview()
        platformStartPreview()
    }

    companion object {
        private const val TAG = "AndroidCamera"
    }
}

internal actual fun createCamera(): Camera = AndroidCamera()
