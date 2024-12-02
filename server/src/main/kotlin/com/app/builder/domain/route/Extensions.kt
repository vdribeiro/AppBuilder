package com.app.builder.domain.route

import kotlin.time.Duration.Companion.milliseconds
import kotlin.uuid.Uuid
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.httpMethod
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import com.app.builder.core.config.ServerConfigs
import com.app.builder.core.locale.now
import com.app.builder.core.locale.toInstant
import com.app.builder.core.platform.Env
import com.app.builder.core.platform.OS
import com.app.builder.core.security.toUuid
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.http.Header
import com.app.builder.domain.EntityType
import com.app.builder.domain.Permission
import com.app.builder.domain.Registry
import com.app.builder.domain.permission.PermissionService
import com.app.builder.domain.permission.getPermissions
import com.auth0.jwt.interfaces.Payload

/**
 * Safely responds to the HTTP request with the provided [message].
 *
 * @receiver [ApplicationCall] The context of the call.
 * @param status The HTTP status code to respond with.
 * @param message The response body to serialize.
 */
suspend inline fun <reified T: Any> ApplicationCall.respondSafely(status: HttpStatusCode, message: T) = runCatching {
    respond(status = status, message = message)
}.onFailure {
    Telemetry.error(tag = "Route", message = "Unable to respond", throwable = it)
}.getOrNull()

/**
 * Safely receives and deserializes the HTTP request body.
 * Logs an error to telemetry if the process fails and responds with a 400 Bad Request.
 *
 * @receiver [ApplicationCall] The context of the call.
 * @param T The expected type to deserialize the request body into.
 * @return [T] The deserialized object, or null if deserialization fails.
 */
suspend inline fun <reified T: Any> ApplicationCall.receive(): T? = runCatching {
    receive<T>()
}.onFailure {
    Telemetry.error(tag = "Route", message = "Unable to process call", throwable = it)
    respondSafely(status = HttpStatusCode.BadRequest, message = "Invalid request payload")
}.getOrNull()

/**
 * Extracts the parameters safely from the HTTP request.
 * Logs an error to telemetry if the process fails and responds with a 400 Bad Request.
 *
 * @receiver [ApplicationCall] The context of the call.
 * @return A [Map] where each key is associated with its joined values.
 */
suspend fun ApplicationCall.getParameters(): Map<String, String> = runCatching {
    parameters.entries().associate { (key, values) -> key to values.joinToString(separator = ",") }
}.onFailure {
    Telemetry.error(tag = TAG, message = "Unable to get parameters map", throwable = it)
    respondSafely(status = HttpStatusCode.BadRequest, message = "Invalid parameters")
}.getOrDefault(defaultValue = emptyMap())

/**
 * Extracts the query map safely from the HTTP request.
 * Logs an error to telemetry if the process fails.
 *
 * @receiver [ApplicationCall] The context of the call.
 * @return A [Map] where each key is associated with its joined values.
 */
fun ApplicationCall.getQueryMap(): Map<String, String> = runCatching {
    request.queryParameters.entries().associate { (key, values) ->
        key to values.joinToString(separator = ",")
    }
}.onFailure {
    Telemetry.error(tag = TAG, message = "Unable to get query map", throwable = it)
}.getOrDefault(defaultValue = emptyMap())

/**
 * Extracts the header map safely from the HTTP request.
 * Logs an error to telemetry if the process fails.
 *
 * @receiver [ApplicationCall] The context of the call.
 * @return A [Map] where each key is associated with its joined values.
 */
fun ApplicationCall.getHeaderMap(): Map<String, String> = runCatching {
    request.headers.entries().associate { (key, values) ->
        key to values.joinToString(separator = ",")
    }
}.onFailure {
    Telemetry.error(tag = TAG, message = "Unable to get header map", throwable = it)
}.getOrDefault(defaultValue = emptyMap())

/**
 * Validates the request time against the server time.
 * Rejects the request with 400 if the timestamp is further in the future than [ServerConfigs.timestampFutureTolerance].
 *
 * @receiver [ApplicationCall] The context of the call.
 * @return `true` if the time is valid, `false` otherwise.
 */
suspend fun ApplicationCall.validateTime(): Boolean = runCatching {
    if (Env.developmentMode) return@runCatching true
    val headerMap = getHeaderMap()
    val requestUtc = headerMap[Header.RequestUtc.header]?.toInstant() ?: error("Request time is missing")
    val requestSentUtc = headerMap[Header.RequestSentUtc.header]?.toInstant() ?: error("Request sent time is missing")
    val future = now() + ServerConfigs.configs.timestampFutureTolerance.milliseconds
    if (requestUtc > future || requestSentUtc > future) error("Time is in the future")
    true
}.onFailure {
    Telemetry.error(tag = TAG, message = "Invalid time", throwable = it)
    respondSafely(status = HttpStatusCode.BadRequest, message = "Invalid time")
}.getOrDefault(defaultValue = false)

/**
 * Safely extracts the JWT payload from the authenticated [ApplicationCall].
 *
 * @receiver [ApplicationCall] The context of the call.
 * @return The [Payload] of the JWT token, or null if the principal is missing or invalid.
 */
private fun ApplicationCall.getJwtPayload(): Payload? = principal<JWTPrincipal>()?.payload

/**
 * Get the user uuid from the JWT payload.
 * Logs an error to telemetry if the process fails.
 *
 * @receiver [ApplicationCall] The context of the call.
 * @return [Uuid] The user uuid, or null if it fails.
 */
suspend fun ApplicationCall.getUserUuid(): Uuid? = runCatching {
    getJwtPayload()!!.subject.toUuid()!!
}.onFailure {
    Telemetry.error(tag = TAG, message = "Unable to get user uuid", throwable = it)
    respondSafely(status = HttpStatusCode.Unauthorized, message = "Invalid token")
}.getOrNull()

/**
 * Get the user permission from the JWT payload.
 * Logs an error to telemetry if the process fails.
 *
 * @receiver [ApplicationCall] The context of the call.
 * @param permissionService The permission service.
 * @return [Map] The user permissions, or null if it fails.
 */
suspend fun ApplicationCall.getPermissions(permissionService: PermissionService): Map<EntityType, Permission>? = runCatching {
    val userUuid = getUserUuid() ?: return null
    permissionService.get(userUuid = userUuid) ?: getJwtPayload()!!.getPermissions()!!
}.onFailure {
    Telemetry.error(tag = TAG, message = "Unable to get user permissions", throwable = it)
    respondSafely(status = HttpStatusCode.Unauthorized, message = "Invalid token")
}.getOrNull()

/**
 * Checks if the user has the required [permission] for the [entityType], responding to the call when they do not.
 * Permissions are taken from [permissionService] so that changes apply before the access token expires, falling back to the token claim when the user is not cached.
 *
 * @receiver [ApplicationCall] The context of the call.
 * @param permissionService The permission service.
 * @param entityType The entity type.
 * @param permission The permission.
 * @return `true` if the user is authorized, `false` otherwise.
 */
suspend fun ApplicationCall.validatePermission(
    permissionService: PermissionService,
    entityType: EntityType,
    permission: Permission
): Boolean = runCatching {
    when (getPermissions(permissionService = permissionService)!![entityType]) {
        Permission.WRITE -> true
        Permission.READ -> permission == Permission.READ
        else -> false
    }.also {
        if (!it) respondSafely(status = HttpStatusCode.Forbidden, message = "Not authorized to $permission $entityType")
    }
}.onFailure {
    Telemetry.error(tag = TAG, message = "Unable to check permissions", throwable = it)
    respondSafely(status = HttpStatusCode.Forbidden, message = "Not authorized to $permission $entityType")
}.getOrDefault(defaultValue = false)

/**
 * Records a registry entry for the current request, sourcing client and device metadata from the request headers and the authenticated user's uuid from the JWT payload.
 * This is best-effort telemetry, not a business invariant: it never touches the response, and any failure (missing header, unparseable value, unauthenticated call) is only logged.
 *
 * @receiver [ApplicationCall] The context of the call.
 * @param entityType The type of entity targeted by the request.
 * @param entityUuid The unique identifier of the entity targeted by the request, if any.
 * @param userUuid The optional user unique identifier.
 * @param payload The request payload to record, if any.
 */
fun ApplicationCall.toRegistry(
    entityType: EntityType,
    entityUuid: Uuid? = null,
    userUuid: Uuid? = null,
    payload: String? = null
): Registry? = runCatching {
    val headerMap = getHeaderMap()
    Registry(
        requestUuid = headerMap[Header.RequestUuid.header]?.toUuid() ?: error("Request uuid is missing"),
        requestType = request.httpMethod.value,
        requestUtc = headerMap[Header.RequestUtc.header]?.toInstant() ?: error("Request time is missing"),
        requestSentUtc = headerMap[Header.RequestSentUtc.header]?.toInstant() ?: error("Request sent time is missing"),
        deletedAt = null,
        userUuid = userUuid ?: getJwtPayload()?.subject?.toUuid() ?: error("User uuid is missing"),
        appVersion = headerMap[Header.AppVersion.header] ?: error("App version is missing"),
        os = headerMap[Header.Os.header]?.let { OS.fromString(value = it) } ?: error("Os is missing"),
        osVersion = headerMap[Header.OsVersion.header] ?: error("Os version is missing"),
        brand = headerMap[Header.Brand.header] ?: error("Brand is missing"),
        model = headerMap[Header.Model.header] ?: error("Model is missing"),
        deviceUuid = headerMap[Header.DeviceUuid.header]?.toUuid() ?: error("Device uuid is missing"),
        entityUuid = entityUuid,
        entityType = entityType,
        payload = payload
    )
}.onFailure {
    Telemetry.error(tag = TAG, message = "Unable to map to registry", throwable = it)
}.getOrNull()

private const val TAG = "Route"
