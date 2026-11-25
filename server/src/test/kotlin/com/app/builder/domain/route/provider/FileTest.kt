package com.app.builder.domain.route.provider

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.ContentDisposition
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import com.app.builder.core.config.ServerConfigs
import com.app.builder.core.config.ServerFlags
import com.app.builder.core.security.uuid
import com.app.builder.data.http.URL
import com.app.builder.domain.EntityType
import com.app.builder.domain.FileMetadata
import com.app.builder.test.TestCase
import io.ktor.client.request.get as ktorGet
import io.ktor.client.request.post as ktorPost

class FileTest: TestCase() {

    override suspend fun beforeTest() {
        super.beforeTest()
        ServerFlags.set { it.copy(files = true) }
    }

    /** Verifies that an uploaded file can be fetched as metadata and downloaded with its name and media type, and that the upload is recorded in the registry. */
    @Test
    fun uploadAndDownload() = runServerTest {
        installRouting()
        val client = createClient(token = adminBearerToken.accessToken)
        val uuid = uuid()
        val content = "file content".encodeToByteArray()

        val upload = client.ktorPost(urlString = "${URL.Files.path}/$uuid") {
            registryHeaders().forEach { (key, value) -> header(key = key.header, value = value) }
            fileName(name = "notes.txt")
            contentType(type = ContentType.Text.Plain)
            setBody(body = content)
        }
        assertEquals(expected = HttpStatusCode.OK, actual = upload.status)
        val file = upload.body<FileMetadata>()
        assertEquals(expected = FileMetadata(uuid = uuid, modifiedAt = file.modifiedAt, name = "notes.txt", contentType = "text/plain", size = content.size.toLong()), actual = file)

        assertEquals(expected = file, actual = client.ktorGet(urlString = "${URL.Files.path}/$uuid").body<FileMetadata>())

        val download = client.ktorGet(urlString = "${URL.Files.path}/$uuid/content")
        assertEquals(expected = HttpStatusCode.OK, actual = download.status)
        assertEquals(expected = ContentType.Text.Plain, actual = download.contentType()?.withoutParameters())
        assertTrue(actual = download.headers[HttpHeaders.ContentDisposition].orEmpty().contains(other = "notes.txt"))
        assertContentEquals(expected = content, actual = download.bodyAsBytes())

        assertTrue(actual = registries().any { it.entityType == EntityType.FILE && it.entityUuid == uuid })
    }

    /** Verifies that the file name is read from a UTF-8 `filename*` or a plain `filename`, and sent back as `filename*` on download. */
    @Test
    fun fileName() = runServerTest {
        installRouting()
        val client = createClient(token = adminBearerToken.accessToken)

        val unicodeUuid = uuid()
        val unicode = client.ktorPost(urlString = "${URL.Files.path}/$unicodeUuid") { fileName(name = "résumé.pdf"); setBody(body = "content") }
        assertEquals(expected = "résumé.pdf", actual = unicode.body<FileMetadata>().name)
        val disposition = ContentDisposition.parse(value = client.ktorGet(urlString = "${URL.Files.path}/$unicodeUuid/content").headers[HttpHeaders.ContentDisposition].orEmpty())
        assertEquals(expected = "utf-8''r%C3%A9sum%C3%A9.pdf", actual = disposition.parameter(name = ContentDisposition.Parameters.FileNameAsterisk))

        val plain = client.ktorPost(urlString = "${URL.Files.path}/${uuid()}") {
            header(key = HttpHeaders.ContentDisposition, value = ContentDisposition.Attachment.withParameter(key = ContentDisposition.Parameters.FileName, value = "notes.txt").toString())
            setBody(body = "content")
        }
        assertEquals(expected = "notes.txt", actual = plain.body<FileMetadata>().name)
    }

    /** Verifies that an unknown file is not found and that an upload without a file name is rejected. */
    @Test
    fun invalidRequests() = runServerTest {
        installRouting()
        val client = createClient(token = adminBearerToken.accessToken)
        val uuid = uuid()

        assertEquals(expected = HttpStatusCode.NotFound, actual = client.ktorGet(urlString = "${URL.Files.path}/$uuid").status)
        assertEquals(expected = HttpStatusCode.NotFound, actual = client.ktorGet(urlString = "${URL.Files.path}/$uuid/content").status)
        assertEquals(expected = HttpStatusCode.BadRequest, actual = client.ktorPost(urlString = "${URL.Files.path}/$uuid") { setBody(body = "content") }.status)
    }

    /** Verifies that downloading needs the file read permission and uploading the file write permission. */
    @Test
    fun permissions() = runServerTest {
        installRouting()
        val uuid = uuid()
        val uploadPath = "${URL.Files.path}/$uuid"
        assertEquals(expected = HttpStatusCode.OK, actual = createClient(token = adminBearerToken.accessToken).ktorPost(urlString = uploadPath) { fileName(name = "notes.txt"); setBody(body = "content") }.status)

        val noPermission = createClient(token = seedUser(registrationForm = registrationForm(username = "no_files", permissions = emptyMap())).bearer.accessToken)
        assertEquals(expected = HttpStatusCode.Forbidden, actual = noPermission.ktorGet(urlString = "${URL.Files.path}/$uuid/content").status)
        assertEquals(expected = HttpStatusCode.Forbidden, actual = noPermission.ktorPost(urlString = uploadPath) { fileName(name = "notes.txt"); setBody(body = "content") }.status)

        val reader = createClient(token = bearerToken.accessToken)
        assertEquals(expected = HttpStatusCode.OK, actual = reader.ktorGet(urlString = "${URL.Files.path}/$uuid/content").status)
        assertEquals(expected = HttpStatusCode.Forbidden, actual = reader.ktorPost(urlString = uploadPath) { fileName(name = "notes.txt"); setBody(body = "content") }.status)
    }

    /** Verifies that uploads follow [ServerConfigs.maxFileSize] in place of [ServerConfigs.maxBodySize]. */
    @Test
    fun uploadFollowsMaxFileSize() = runServerTest {
        installRouting()
        ServerConfigs.set { it.copy(maxBodySize = 8L, maxFileSize = 16L) }
        val client = createClient(token = adminBearerToken.accessToken)
        val uploadPath = "${URL.Files.path}/${uuid()}"

        assertEquals(expected = HttpStatusCode.OK, actual = client.ktorPost(urlString = uploadPath) { fileName(name = "notes.txt"); setBody(body = "a".repeat(n = 16)) }.status)
        assertEquals(expected = HttpStatusCode.PayloadTooLarge, actual = client.ktorPost(urlString = uploadPath) { fileName(name = "notes.txt"); setBody(body = "a".repeat(n = 17)) }.status)
    }

    /** Verifies that the file routes are not served while the files flag is off. */
    @Test
    fun flagOff() = runServerTest {
        installRouting()
        ServerFlags.set { it.copy(files = false) }
        val client = createClient(token = adminBearerToken.accessToken)

        assertEquals(expected = HttpStatusCode.NotFound, actual = client.ktorGet(urlString = "${URL.Files.path}/${uuid()}").status)
    }

    /**
     * Names the uploaded file through a UTF-8 `filename*` in the `Content-Disposition` header.
     *
     * @param name The file name.
     */
    private fun HttpRequestBuilder.fileName(name: String) {
        header(key = HttpHeaders.ContentDisposition, value = ContentDisposition.Attachment.withParameter(key = ContentDisposition.Parameters.FileNameAsterisk, value = name).toString())
    }
}
