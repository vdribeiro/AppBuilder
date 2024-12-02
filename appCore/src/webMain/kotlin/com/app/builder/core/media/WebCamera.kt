package com.app.builder.core.media

import kotlinx.browser.document
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import com.app.builder.core.media.WebCamera.Companion.FINALIZE_TIMEOUT
import com.app.builder.core.platform.loop
import com.app.builder.core.telemetry.Telemetry
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLVideoElement

/** Web [Camera] implementation backed by `navigator.mediaDevices` and a `MediaRecorder`. */
internal class WebCamera: Camera() {

    /** Scope for the MediaRecorder. */
    private val scope = CoroutineScope(context = SupervisorJob())

    /**
     * Detached video element used as the render target for the live camera stream.
     * `autoplay` and `playsinline` are set so the stream plays immediately without user interaction and without going fullscreen on mobile browsers.
     */
    private var videoElement: HTMLVideoElement? = null
    /** Wrapper `div`. */
    private var container: HTMLDivElement? = null
    /** The callback given to [platformStartRecording], invoked once [platformStopRecording]'s polling loop resolves the recording's object URL. */
    private var onRecordingResult: ((String) -> Unit)? = null
    /** True while a previous recording's finalize-polling loop is still running. Guards against a new recording overwriting the shared globals before that poll reads them. */
    private var finalizing: Boolean = false
    /** The active job polling for the stream ending unexpectedly (e.g. camera permission revoked mid-preview), while previewing. */
    private var streamWatchJob: Job? = null

    override val available: Boolean = runCatching {
        hasMediaDevicesSupport()
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable to check camera availability", throwable = it)
    }.getOrDefault(defaultValue = false)

    /**
     * Initializes the video element.
     *
     * @param width Width of the video element.
     * @param height Height of the video element.
     * @return The initialized container element, or null if an error occurred.
     */
    fun init(
        width: String = "100%",
        height: String = "100%",
    ): HTMLDivElement? = runCatching {
        container?.let { return it }

        val container = document.createElement(localName = "div") as? HTMLDivElement ?: return null
        container.style.width = "100%"
        container.style.height = "100%"
        container.style.setProperty(property = "overflow", value = "hidden")
        videoElement = (document.createElement(localName = "video") as HTMLVideoElement).also {
            it.setAttribute(qualifiedName = "autoplay", value = "")
            it.setAttribute(qualifiedName = "playsinline", value = "")
        }.apply {
            style.width = width
            style.height = height
            style.setProperty(property = "object-fit", value = "cover")
            container.appendChild(node = this)
        }
        this.container = container
        document.body?.appendChild(node = container)

        container
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable to initialize HTMLVideoElement", throwable = it)
    }.getOrNull()

    override fun platformDispose() {
        super.platformDispose()
        container?.let { document.body?.removeChild(child = it) }
        container = null
        videoElement = null
        onRecordingResult = null
        streamWatchJob = null
    }

    override fun hasPermission(): Boolean {
        super.hasPermission()
        // The browser prompts for access at the point of use.
        return true
    }

    /** Calls `navigator.mediaDevices.getUserMedia` with the appropriate `facingMode` constraint ("environment" for back, "user" for front) and assigns the resulting stream to video.srcObject. Also watches for the stream ending unexpectedly (e.g. permission denied/revoked), which stops the preview. */
    override fun platformStartPreview() {
        super.platformStartPreview()
        val video = this@WebCamera.videoElement ?: error(message = "No video element")
        val facingMode = if (facing == Facing.BACK) "environment" else "user"
        startMediaStream(video = video, facingMode = facingMode)
        streamWatchJob = scope.launch {
            loop { if (pollStreamEnded()) return@loop stopPreview() }
        }
    }

    /** Stops any active MediaRecorder then stops all tracks on the stream and clears video.srcObject, releasing the camera hardware back to the browser. */
    override fun platformStopPreview() {
        super.platformStopPreview()
        streamWatchJob?.cancel()
        streamWatchJob = null
        stopMediaRecorder()
        val video = this@WebCamera.videoElement ?: error(message = "No video element")
        stopMediaStream(video = video)
    }

    /**
     * Draws the current video frame onto an off-screen [HTMLCanvasElement] and invokes [onResult] with the result as a JPEG data URL, or null if no frame is available.
     *
     * @param onResult Invoked with the captured frame as a JPEG data URL, or null if no frame was available.
     */
    override fun platformCapturePhoto(onResult: (String?) -> Unit) {
        super.platformCapturePhoto(onResult = onResult)
        val video = this@WebCamera.videoElement ?: return onResult(null)
        if (video.videoWidth == 0 || video.videoHeight == 0) return onResult(null)
        val canvas = document.createElement(localName = "canvas") as HTMLCanvasElement
        canvas.width = video.videoWidth
        canvas.height = video.videoHeight
        onResult(captureFrame(video = video, canvas = canvas))
    }

    /**
     * Creates a MediaRecorder on the active stream and starts collecting encoded chunks into `window.__cameraChunks`. The recorder reference is kept in `window.__cameraMediaRecorder`.
     * [onResult] is invoked once [platformStopRecording]'s polling loop resolves the recording's object URL.
     *
     * @param onResult Invoked with the recording's object URL once [platformStopRecording] resolves it. Stored in [onRecordingResult] until then.
     */
    override fun platformStartRecording(onResult: (String) -> Unit) {
        super.platformStartRecording(onResult = onResult)
        if (finalizing) return
        val video = this@WebCamera.videoElement ?: error(message = "No video element")
        startMediaRecorder(video = video)
        onRecordingResult = onResult
    }

    /**
     * Stops the MediaRecorder and polls `window.__cameraRecordingUrl` until the asynchronous `onstop` callback has written a blob object URL to that global, or [FINALIZE_TIMEOUT] elapses.
     * Once resolved, the URL is cleared from the global and passed to the `onResult` callback given to [platformStartRecording]. A timed-out finalization logs an error instead of invoking `onResult`.
     * Polling is necessary because `MediaRecorder.stop()` finalizes the blob asynchronously on the JS event loop, so the URL is not immediately available after calling stop.
     */
    override fun platformStopRecording() {
        super.platformStopRecording()
        stopMediaRecorder()
        val onResult = onRecordingResult ?: error(message = "No onResult callback")
        onRecordingResult = null
        finalizing = true
        scope.launch {
            val url = withTimeoutOrNull(timeMillis = FINALIZE_TIMEOUT) {
                // Poll until the callback writes the object URL.
                var url = pollRecordingUrl()
                while (url.isEmpty()) {
                    delay(timeMillis = 50)
                    url = pollRecordingUrl()
                }
                url
            }
            clearRecordingUrl()
            finalizing = false
            when (url) {
                null -> Telemetry.error(tag = TAG, message = "Timed out waiting for the recording to finalize")
                else -> onResult(url)
            }
        }
    }

    /** Stops the current stream and restarts it with the new [facing] direction, which causes `getUserMedia` to re-negotiate the `facingMode` constraint with the browser. */
    override fun platformToggleFacing() {
        super.platformToggleFacing()
        platformStopPreview()
        platformStartPreview()
    }

    companion object {
        private const val TAG = "WebCamera"
        /** Maximum time to wait for [platformStopRecording]'s finalize-polling loop before giving up. */
        private const val FINALIZE_TIMEOUT = 20_000L
    }
}

/** Returns whether the browser exposes the `getUserMedia` API needed to access a camera. */
@JsFun(code = "() => !!(navigator.mediaDevices && navigator.mediaDevices.getUserMedia)")
private external fun hasMediaDevicesSupport(): Boolean

/** Calls `navigator.mediaDevices.getUserMedia` and sets the resulting stream on video.srcObject. Marks `window.__cameraStreamEnded` when the request fails or a granted track later ends on its own (e.g. permission denied/revoked), for [pollStreamEnded] to observe. */
@JsFun(
    code = """(video, facingMode) => {
        window.__cameraStreamEnded = false;
        navigator.mediaDevices.getUserMedia({ video: { facingMode: facingMode }, audio: false }).then(function(s) {
            video.srcObject = s;
            s.getVideoTracks().forEach(function(t) { t.onended = function() { window.__cameraStreamEnded = true; }; });
        }).catch(function() { window.__cameraStreamEnded = true; });
    }"""
)
private external fun startMediaStream(video: HTMLVideoElement, facingMode: String)

/** Returns and clears whether the active camera stream ended unexpectedly (e.g. permission denied/revoked) since the last poll. */
@JsFun(code = "() => { var e = !!window.__cameraStreamEnded; window.__cameraStreamEnded = false; return e; }")
private external fun pollStreamEnded(): Boolean

/** Stops all tracks on video.srcObject and nulls the reference, releasing the camera device. */
@JsFun(code = "(video) => { if (video.srcObject) { video.srcObject.getTracks().forEach(function(t) { t.onended = null; t.stop(); }); video.srcObject = null; } window.__cameraStreamEnded = false; }")
private external fun stopMediaStream(video: HTMLVideoElement)

/** Draws [video]'s current frame onto [canvas] and returns the result as a JPEG data URL. */
@JsFun(code = "(video, canvas) => { canvas.getContext('2d').drawImage(video, 0, 0); return canvas.toDataURL('image/jpeg'); }")
private external fun captureFrame(video: HTMLVideoElement, canvas: HTMLCanvasElement): String

/** Creates a `MediaRecorder` on `video.srcObject`, collects encoded chunks in `window.__cameraChunks`, and on `onstop` assembles them into a `webm` blob whose object URL is stored in `window.__cameraRecordingUrl` for [pollRecordingUrl] to read. */
@JsFun(
    code = """(video) => {
        window.__cameraChunks = [];
        window.__cameraRecordingUrl = '';
        if (!video.srcObject) return;
        var mr = new MediaRecorder(video.srcObject);
        mr.ondataavailable = function(e) { if (e.data && e.data.size > 0) window.__cameraChunks.push(e.data); };
        mr.onstop = function() {
            var blob = new Blob(window.__cameraChunks, { type: 'video/webm' });
            window.__cameraRecordingUrl = URL.createObjectURL(blob);
            window.__cameraChunks = [];
        };
        mr.start();
        window.__cameraMediaRecorder = mr;
    }"""
)
private external fun startMediaRecorder(video: HTMLVideoElement)

/** Stops `window.__cameraMediaRecorder` if it is not already inactive, then nulls the reference. */
@JsFun(
    code = """() => {
        if (window.__cameraMediaRecorder && window.__cameraMediaRecorder.state !== 'inactive') {
            window.__cameraMediaRecorder.stop();
        }
        window.__cameraMediaRecorder = null;
    }"""
)
private external fun stopMediaRecorder()

/** Returns the object URL written by the `onstop` callback, or an empty string if not yet available. */
@JsFun(code = "() => window.__cameraRecordingUrl || ''")
private external fun pollRecordingUrl(): String

/** Clears `window.__cameraRecordingUrl` after the URL has been consumed by [WebCamera.platformStopRecording]. */
@JsFun(code = "() => { window.__cameraRecordingUrl = ''; }")
private external fun clearRecordingUrl()

internal actual fun createCamera(): Camera = WebCamera()
