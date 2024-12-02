package com.app.builder.data.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import com.app.builder.core.telemetry.Telemetry

/** A centralized [Json] instance configured with a lenient mapping policy to maintain resilience against evolving API schemas and data structures. */
val json = Json {
    isLenient = true
    ignoreUnknownKeys = true
    encodeDefaults = true
    allowTrailingComma = true
}

/**
 * Serializes an object of type [T] into a JSON [String].
 *
 * @param T The type of the object to encode.
 * @param jsonSerializer The [Json] instance containing parsing rules. Defaults to [json].
 * @param value The object instance to serialize.
 * @return A JSON formatted string, or null if serialization fails.
 */
inline fun <reified T> encode(jsonSerializer: Json = json, value: T): String? = runCatching {
    jsonSerializer.encodeToString(value = value)
}.onFailure {
    Telemetry.error(tag = "JSON", message = "Unable to encode value", throwable = it)
}.getOrNull()

/**
 * Serializes an object of type [T] into a JSON [String] using an explicitly supplied [serializer].
 *
 * @param T The type of the object to encode.
 * @param jsonSerializer The [Json] instance containing parsing rules. Defaults to [json].
 * @param serializer The strategy describing how to write [T].
 * @param value The object instance to serialize.
 * @return A JSON formatted string, or null if serialization fails.
 */
fun <T> encode(jsonSerializer: Json = json, serializer: KSerializer<T>, value: T): String? = runCatching {
    jsonSerializer.encodeToString(serializer = serializer, value = value)
}.onFailure {
    Telemetry.error(tag = "JSON", message = "Unable to encode value", throwable = it)
}.getOrNull()

/**
 * Deserializes a JSON [String] back into an object of type [T].
 * Includes a structural fallback policy for blank strings:
 * evaluates and handles empty string blocks as empty collection or object literals to mitigate unexpected parsing crashes.
 *
 * @param T The expected target data model type.
 * @param jsonSerializer The [Json] instance containing parsing rules. Defaults to [json].
 * @param value The raw JSON string to deserialize.
 * @return A parsed instance of [T], or null if deserialization fails.
 */
inline fun <reified T> decode(jsonSerializer: Json = json, value: String): T? = runCatching {
    jsonSerializer.decodeFromString<T>(string = value.ifBlank {
        when (T::class) {
            Collection::class, List::class, Set::class -> "[]"
            else -> "{}"
        }
    })
}.onFailure {
    Telemetry.error(tag = "JSON", message = "Unable to decode value", throwable = it)
}.getOrNull()

/**
 * Deserializes a JSON [String] back into an object of type [T] using an explicitly supplied [serializer].
 * Includes a structural fallback policy for blank strings:
 * evaluates and handles empty string blocks as empty collection or object literals to mitigate unexpected parsing crashes.
 *
 * @param T The expected target data model type.
 * @param jsonSerializer The [Json] instance containing parsing rules. Defaults to [json].
 * @param serializer The strategy describing how to read [T].
 * @param value The raw JSON string to deserialize.
 * @return A parsed instance of [T], or null if deserialization fails.
 */
fun <T> decode(jsonSerializer: Json = json, serializer: KSerializer<T>, value: String): T? = runCatching {
    jsonSerializer.decodeFromString(string = value.ifBlank {
        when (serializer.descriptor.kind) {
            StructureKind.LIST -> "[]"
            else -> "{}"
        }
    }, deserializer = serializer)
}.onFailure {
    Telemetry.error(tag = "JSON", message = "Unable to decode value", throwable = it)
}.getOrNull()

/**
 * Serializes an object of type [T] into a [JsonElement].
 *
 * @param T The type of the object to encode.
 * @param jsonSerializer The [Json] instance containing parsing rules. Defaults to [json].
 * @param value The object instance to serialize.
 * @return A [JsonElement], or null if serialization fails.
 */
inline fun <reified T> encodeToJson(jsonSerializer: Json = json, value: T): JsonElement? = runCatching {
    jsonSerializer.encodeToJsonElement(value = value)
}.onFailure {
    Telemetry.error(tag = "JSON", message = "Unable to encode value", throwable = it)
}.getOrNull()

/**
 * Deserializes a [JsonElement] back into an object of type [T].
 *
 * @param T The expected target data model type.
 * @param jsonSerializer The [Json] instance containing parsing rules. Defaults to [json].
 * @param value The [JsonElement] to deserialize.
 * @return A parsed instance of [T], or null if deserialization fails.
 */
inline fun <reified T> decodeFromJson(jsonSerializer: Json = json, value: JsonElement): T? = runCatching {
    jsonSerializer.decodeFromJsonElement<T>(json = value)
}.onFailure {
    Telemetry.error(tag = "JSON", message = "Unable to decode value", throwable = it)
}.getOrNull()