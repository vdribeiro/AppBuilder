package com.app.builder.data.resource

import kotlinx.coroutines.withContext
import appbuilder.clientapp.generated.resources.Res
import com.app.builder.core.flow.Dispatcher
import com.app.builder.core.flow.runSuspendCatching
import com.app.builder.core.telemetry.Telemetry

/**
 * Loads the text content of a bundled JSON asset.
 * Executes file I/O on a background thread. If the asset is missing or unreadable, the exception is logged and null is returned to prevent runtime crashes.
 *
 * @param json The [JsonResource] registry object containing the asset's file path.
 * @return The raw JSON content on success, or null upon failure.
 */
internal suspend fun loadResource(json: JsonResource): String? = withContext(context = Dispatcher.IO) {
    runSuspendCatching {
        Res.readBytes(path = json.path).decodeToString()
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable to load resource", throwable = it)
    }.getOrNull()
}

private const val TAG = "ResourceLoader"
