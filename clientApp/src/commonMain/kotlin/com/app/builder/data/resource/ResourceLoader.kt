package com.app.builder.data.resource

import kotlinx.coroutines.withContext
import appbuilder.clientapp.generated.resources.Res
import com.app.builder.core.flow.Dispatcher
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.serializer.decode

/**
 * Loads and deserializes a bundled JSON asset into a structured list of type [T].
 * Executes file I/O and deserialization on a background thread. If the asset is missing, malformed, or fails to match the expected schema, the exception is logged and an empty list is returned to prevent runtime crashes.
 *
 * @param T The target domain model type expected within the JSON array.
 * @param json The [JsonResource] registry object containing the asset's file path.
 * @return A list of deserialized [T] objects on success, or an empty list upon failure.
 */
internal suspend inline fun <reified T> loadResource(json: JsonResource): List<T> = withContext(context = Dispatcher.IO) {
    runCatching {
        decode<List<T>>(value = Res.readBytes(path = json.path).decodeToString())
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable to load resource", throwable = it)
    }.getOrNull().orEmpty()
}

private const val TAG = "ResourceLoader"
