package com.app.builder.domain.storage

import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.time.Duration
import kotlinx.coroutines.future.await
import com.app.builder.core.flow.Dispatcher
import com.app.builder.core.locale.now
import com.app.builder.core.telemetry.Telemetry
import kotlinx.coroutines.withContext

/**
 * An [ObjectStorageService] backed by an S3 compatible bucket, addressed path style as `endpoint/bucket/key`.
 * Requests carry a Signature Version 4 signature in the query string, so an upload URL can be handed to a client that holds no credentials.
 *
 * @property endpoint The bucket host, without a bucket or trailing slash.
 * @property bucket The bucket holding the objects.
 * @property region The region scoping the signature.
 * @property accessKeyId The access key identifying the signer.
 * @property secretAccessKey The secret the signature is derived from.
 */
class S3ObjectStorage(
    private val endpoint: String,
    private val bucket: String,
    private val region: String,
    private val accessKeyId: String,
    private val secretAccessKey: String
): ObjectStorageService {

    /** Issues the signed requests this server makes on its own behalf. */
    private val client: HttpClient = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build()

    override fun presignUpload(key: String, contentType: String, expiry: Duration): String? = runCatching {
        presign(method = "PUT", key = key, contentType = contentType, expiry = expiry)
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable to sign an upload URL for $key", throwable = it)
    }.getOrNull()

    override suspend fun exists(key: String): Boolean = send(method = "HEAD", key = key) { it in 200..299 }

    override suspend fun delete(key: String): Boolean = send(method = "DELETE", key = key) { it in 200..299 || it == 404 }

    /**
     * Sends a signed request this server makes on its own behalf and judges the response.
     *
     * @param method The HTTP method to sign and send.
     * @param key The object key to act on.
     * @param success Decides whether a status code counts as success.
     * @return The verdict of [success], or `false` if the request could not be made.
     */
    private suspend fun send(method: String, key: String, success: (Int) -> Boolean): Boolean = withContext(context = Dispatcher.IO) {
        runCatching {
            val request = HttpRequest.newBuilder()
                .uri(URI.create(presign(method = method, key = key, contentType = null, expiry = SELF_REQUEST_EXPIRY)))
                .timeout(REQUEST_TIMEOUT)
                .method(method, HttpRequest.BodyPublishers.noBody())
                .build()
            val response = client.sendAsync(request, HttpResponse.BodyHandlers.discarding()).await()
            success(response.statusCode())
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to $method $key", throwable = it)
        }.getOrDefault(defaultValue = false)
    }

    /**
     * Signs a single request into a URL that carries its own authorisation.
     * Signing the content type as well as the host binds the URL to one media type, so a granted upload cannot be reused to store something else.
     *
     * @param method The HTTP method the URL is valid for.
     * @param key The object key the URL addresses.
     * @param contentType The media type the caller must declare, or null to sign only the host.
     * @param expiry How long the URL stays valid.
     * @return The signed URL.
     */
    private fun presign(method: String, key: String, contentType: String?, expiry: Duration): String {
        val timestamp = ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(now().toEpochMilliseconds()), ZoneOffset.UTC)
        val amzDate = timestamp.format(AMZ_DATE)
        val dateStamp = timestamp.format(DATE_STAMP)
        val scope = "$dateStamp/$region/$SERVICE/$TERMINATOR"
        val host = URI.create(endpoint).host
        val path = "/$bucket/${key.split("/").joinToString(separator = "/") { encode(value = it) }}"

        val signedHeaders = if (contentType == null) "host" else "content-type;host"
        val canonicalHeaders = buildString {
            if (contentType != null) append("content-type:$contentType\n")
            append("host:$host\n")
        }
        val query = sortedMapOf(
            "X-Amz-Algorithm" to ALGORITHM,
            "X-Amz-Credential" to "$accessKeyId/$scope",
            "X-Amz-Date" to amzDate,
            "X-Amz-Expires" to expiry.inWholeSeconds.toString(),
            "X-Amz-SignedHeaders" to signedHeaders
        ).map { (name, value) -> "${encode(value = name)}=${encode(value = value)}" }.joinToString(separator = "&")

        val canonicalRequest = listOf(method, path, query, canonicalHeaders, signedHeaders, UNSIGNED_PAYLOAD).joinToString(separator = "\n")
        val stringToSign = listOf(ALGORITHM, amzDate, scope, hex(bytes = sha256(value = canonicalRequest))).joinToString(separator = "\n")
        val signature = hex(bytes = hmac(key = signingKey(dateStamp = dateStamp), value = stringToSign))

        return "$endpoint$path?$query&X-Amz-Signature=$signature"
    }

    /**
     * Derives the key a signature for a given day is computed with.
     *
     * @param dateStamp The day the signature is scoped to.
     * @return The derived signing key.
     */
    private fun signingKey(dateStamp: String): ByteArray {
        val date = hmac(key = "AWS4$secretAccessKey".toByteArray(StandardCharsets.UTF_8), value = dateStamp)
        val regional = hmac(key = date, value = region)
        val service = hmac(key = regional, value = SERVICE)
        return hmac(key = service, value = TERMINATOR)
    }

    /**
     * Computes an HMAC-SHA256.
     *
     * @param key The key to sign with.
     * @param value The value to sign.
     * @return The raw signature.
     */
    private fun hmac(key: ByteArray, value: String): ByteArray = Mac.getInstance(MAC_ALGORITHM).run {
        init(SecretKeySpec(key, MAC_ALGORITHM))
        doFinal(value.toByteArray(StandardCharsets.UTF_8))
    }

    /**
     * Computes a SHA-256 digest.
     *
     * @param value The value to digest.
     * @return The raw digest.
     */
    private fun sha256(value: String): ByteArray =
        MessageDigest.getInstance(DIGEST_ALGORITHM).digest(value.toByteArray(StandardCharsets.UTF_8))

    /**
     * Renders bytes as lowercase hexadecimal.
     *
     * @param bytes The bytes to render.
     * @return The hexadecimal string.
     */
    private fun hex(bytes: ByteArray): String = bytes.joinToString(separator = "") { "%02x".format(it) }

    /**
     * Percent encodes a single path segment or query component the way a signature expects, which leaves more characters untouched than form encoding does.
     *
     * @param value The value to encode.
     * @return The encoded value.
     */
    private fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8)
        .replace(oldValue = "+", newValue = "%20")
        .replace(oldValue = "*", newValue = "%2A")
        .replace(oldValue = "%7E", newValue = "~")

    private companion object {
        private const val TAG = "S3ObjectStorage"
        private const val ALGORITHM = "AWS4-HMAC-SHA256"
        private const val SERVICE = "s3"
        private const val TERMINATOR = "aws4_request"
        private const val MAC_ALGORITHM = "HmacSHA256"
        private const val DIGEST_ALGORITHM = "SHA-256"

        /** Tells the signature not to cover the body, so the caller may send bytes the signer never saw. */
        private const val UNSIGNED_PAYLOAD = "UNSIGNED-PAYLOAD"

        private val AMZ_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
        private val DATE_STAMP: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")

        private val CONNECT_TIMEOUT: java.time.Duration = java.time.Duration.ofSeconds(10)
        private val REQUEST_TIMEOUT: java.time.Duration = java.time.Duration.ofSeconds(15)

        /** Requests this server signs and sends itself are used immediately, so they need only a brief window. */
        private val SELF_REQUEST_EXPIRY: Duration = kotlin.time.Duration.parse(value = "60s")
    }
}
