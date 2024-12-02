package com.app.builder.domain.route

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.toJavaInstant
import kotlin.uuid.Uuid
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import com.app.builder.core.locale.now
import com.app.builder.core.platform.Env
import com.app.builder.core.security.uuid
import com.app.builder.data.http.Header
import com.app.builder.domain.EntityType
import com.app.builder.domain.Permission
import com.app.builder.domain.Registry
import com.app.builder.domain.Task
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm

class ExtensionsTest: TestCase() {

    /** Verifies that receive() deserializes a valid body and rejects a malformed one with 400. */
    @Test
    fun receive() = runServerTest {
        installRouting()
        application {
            routing {
                post("/test/receive") {
                    val task = call.receive<Task>() ?: return@post
                    call.respondSafely(status = HttpStatusCode.OK, message = task)
                }
            }
        }

        val client = createClient(token = null)
        val invalidResponse = client.post(urlString = "/test/receive") {
            contentType(type = ContentType.Application.Json)
            setBody(body = "not a task")
        }
        assertEquals(expected = HttpStatusCode.BadRequest, actual = invalidResponse.status)

        val validResponse = client.post(urlString = "/test/receive") {
            contentType(type = ContentType.Application.Json)
            setBody(body = FakeData.task)
        }
        assertEquals(expected = HttpStatusCode.OK, actual = validResponse.status)
        assertEquals(expected = FakeData.task, actual = validResponse.body<Task>())
    }

    /** Verifies that getUserUuid() extracts the subject from a valid token and rejects a token whose subject is not a uuid. */
    @Test
    fun getUserUuid() = runServerTest {
        installRouting()
        application {
            routing {
                authenticate {
                    get("/test/user-uuid") {
                        val userUuid = call.getUserUuid() ?: return@get
                        call.respondSafely(status = HttpStatusCode.OK, message = userUuid)
                    }
                }
            }
        }

        val client = createClient(token = adminBearerToken.accessToken)
        val response = client.get(urlString = "/test/user-uuid")
        assertEquals(expected = HttpStatusCode.OK, actual = response.status)
        assertEquals(expected = FakeData.adminUser.uuid, actual = response.body<Uuid>())

        val malformedSubjectToken = JWT.create()
            .withSubject("not-a-uuid")
            .withAudience(Env.jwtAudience)
            .withIssuer(Env.jwtIssuer)
            .withExpiresAt((now() + 60_000L.milliseconds).toJavaInstant())
            .sign(Algorithm.HMAC512(Env.jwtSecret))

        val malformedClient = createClient(token = malformedSubjectToken)
        assertEquals(expected = HttpStatusCode.Unauthorized, actual = malformedClient.get(urlString = "/test/user-uuid").status)
    }

    /** Verifies that getPermissions() falls back to the JWT claim when the permission service has no cached entry, and prefers the cache once one is set. */
    @Test
    fun getPermissions() = runServerTest {
        installRouting()
        val permissionService = dependency.get().permissionService
        application {
            routing {
                authenticate {
                    get("/test/permissions") {
                        val permissions = call.getPermissions(permissionService = permissionService) ?: return@get
                        call.respondSafely(status = HttpStatusCode.OK, message = permissions)
                    }
                }
            }
        }

        val client = createClient(token = adminBearerToken.accessToken)
        val fallbackResponse = client.get(urlString = "/test/permissions")
        assertEquals(expected = HttpStatusCode.OK, actual = fallbackResponse.status)
        assertEquals(expected = FakeData.adminUser.permissions, actual = fallbackResponse.body<Map<EntityType, Permission>>())

        val cachedPermissions = mapOf(EntityType.TASK to Permission.READ)
        permissionService.set(userUuid = FakeData.adminUser.uuid, permissions = cachedPermissions)
        val cachedResponse = client.get(urlString = "/test/permissions")
        assertEquals(expected = HttpStatusCode.OK, actual = cachedResponse.status)
        assertEquals(expected = cachedPermissions, actual = cachedResponse.body<Map<EntityType, Permission>>())
    }

    /** Verifies that validatePermission() allows write access, restricts read-only access to read requests, and denies access when there is no permission for the entity type. */
    @Test
    fun validatePermission() = runServerTest {
        installRouting()
        val permissionService = dependency.get().permissionService
        application {
            routing {
                authenticate {
                    get("/test/validate-write") {
                        if (!call.validatePermission(permissionService = permissionService, entityType = EntityType.TASK, permission = Permission.WRITE)) return@get
                        call.respondSafely(status = HttpStatusCode.OK, message = "authorized")
                    }
                    get("/test/validate-read") {
                        if (!call.validatePermission(permissionService = permissionService, entityType = EntityType.TASK, permission = Permission.READ)) return@get
                        call.respondSafely(status = HttpStatusCode.OK, message = "authorized")
                    }
                    get("/test/validate-notification-write") {
                        if (!call.validatePermission(permissionService = permissionService, entityType = EntityType.NOTIFICATION, permission = Permission.WRITE)) return@get
                        call.respondSafely(status = HttpStatusCode.OK, message = "authorized")
                    }
                }
            }
        }

        // FakeData.user only has TASK -> READ, and no permission at all for NOTIFICATION.
        val userClient = createClient(token = bearerToken.accessToken)
        assertEquals(expected = HttpStatusCode.Forbidden, actual = userClient.get(urlString = "/test/validate-write").status)
        assertEquals(expected = HttpStatusCode.OK, actual = userClient.get(urlString = "/test/validate-read").status)
        assertEquals(expected = HttpStatusCode.Forbidden, actual = userClient.get(urlString = "/test/validate-notification-write").status)

        // FakeData.adminUser has WRITE for every entity type.
        val adminClient = createClient(token = adminBearerToken.accessToken)
        assertEquals(expected = HttpStatusCode.OK, actual = adminClient.get(urlString = "/test/validate-write").status)
        assertEquals(expected = HttpStatusCode.OK, actual = adminClient.get(urlString = "/test/validate-read").status)
    }

    /** Verifies that toRegistry() returns null when the registry headers are missing, and correctly assembles a [Registry] when they are present. */
    @Test
    fun toRegistry() = runServerTest {
        installRouting()
        val entityUuid = uuid()
        val userUuid = uuid()
        application {
            routing {
                get("/test/registry") {
                    val registry = call.toRegistry(entityType = EntityType.TASK, entityUuid = entityUuid, userUuid = userUuid, payload = "payload")
                        ?: run {
                            call.respondSafely(status = HttpStatusCode.NotFound, message = "no registry")
                            return@get
                        }
                    call.respondSafely(status = HttpStatusCode.OK, message = registry)
                }
            }
        }

        val client = createClient(token = null)
        assertEquals(expected = HttpStatusCode.NotFound, actual = client.get(urlString = "/test/registry").status)

        val requestUuid = uuid()
        val deviceUuid = uuid()
        val requestUtc = now()
        val response = client.get(urlString = "/test/registry") {
            header(key = Header.RequestUuid.header, value = requestUuid.toString())
            header(key = Header.RequestUtc.header, value = requestUtc.toString())
            header(key = Header.RequestSentUtc.header, value = requestUtc.toString())
            header(key = Header.AppVersion.header, value = "1.0.0")
            header(key = Header.Os.header, value = "android")
            header(key = Header.OsVersion.header, value = "14")
            header(key = Header.Brand.header, value = "Google")
            header(key = Header.Model.header, value = "Pixel 8")
            header(key = Header.DeviceUuid.header, value = deviceUuid.toString())
        }
        assertEquals(expected = HttpStatusCode.OK, actual = response.status)

        val registry = response.body<Registry>()
        assertEquals(expected = requestUuid, actual = registry.requestUuid)
        assertEquals(expected = "GET", actual = registry.requestType)
        assertEquals(expected = userUuid, actual = registry.userUuid)
        assertEquals(expected = "1.0.0", actual = registry.appVersion)
        assertEquals(expected = deviceUuid, actual = registry.deviceUuid)
        assertEquals(expected = entityUuid, actual = registry.entityUuid)
        assertEquals(expected = EntityType.TASK, actual = registry.entityType)
        assertEquals(expected = "payload", actual = registry.payload)
    }
}
