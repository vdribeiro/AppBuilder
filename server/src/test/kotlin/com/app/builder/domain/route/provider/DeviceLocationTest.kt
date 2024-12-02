package com.app.builder.domain.route.provider

import kotlin.test.Test
import kotlin.test.assertEquals
import io.ktor.http.HttpStatusCode
import com.app.builder.core.security.uuid
import com.app.builder.data.http.HttpRequest
import com.app.builder.data.http.URL
import com.app.builder.data.http.post
import com.app.builder.domain.EntityType
import com.app.builder.domain.Permission
import com.app.builder.domain.RegistrationForm
import com.app.builder.domain.UserCredentials
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class DeviceLocationTest: TestCase() {

    /** Verifies that reporting a device location requires authentication, rejects invalid bodies and out-of-range coordinates, and succeeds otherwise. */
    @Test
    fun upsertDeviceLocation() = runServerTest {
        installRouting()

        val client = createClient(token = null)
        assertEquals(expected = HttpStatusCode.Unauthorized, actual = client.post(request = HttpRequest(url = URL.DeviceLocations), body = FakeData.deviceLocation).status)

        val authenticatedClient = createClient(token = bearerToken.accessToken)
        assertEquals(expected = HttpStatusCode.BadRequest, actual = authenticatedClient.post(request = HttpRequest(url = URL.DeviceLocations), body = "invalid body").status)

        val invalidLatitude = FakeData.deviceLocation.copy(uuid = uuid(), latitude = 200.0)
        assertEquals(expected = HttpStatusCode.BadRequest, actual = authenticatedClient.post(request = HttpRequest(url = URL.DeviceLocations), body = invalidLatitude).status)

        val invalidLongitude = FakeData.deviceLocation.copy(uuid = uuid(), longitude = -200.0)
        assertEquals(expected = HttpStatusCode.BadRequest, actual = authenticatedClient.post(request = HttpRequest(url = URL.DeviceLocations), body = invalidLongitude).status)

        val response = authenticatedClient.post(request = HttpRequest(url = URL.DeviceLocations), body = FakeData.deviceLocation)
        assertEquals(expected = HttpStatusCode.OK, actual = response.status)
    }

    /** Verifies that reporting a device location is forbidden for a user without device location write permission. */
    @Test
    fun upsertDeviceLocationRequiresWritePermission() = runServerTest {
        installRouting()

        val readOnlyUser = FakeData.user.copy(uuid = uuid(), permissions = mapOf(EntityType.DEVICE_LOCATION to Permission.READ))
        val readOnlyAuth = seedUser(
            registrationForm = RegistrationForm(
                user = readOnlyUser,
                credentials = UserCredentials(username = "readonly_device_location", password = "pass123")
            )
        )

        val client = createClient(token = readOnlyAuth.bearer.accessToken)
        val response = client.post(request = HttpRequest(url = URL.DeviceLocations), body = FakeData.deviceLocation)
        assertEquals(expected = HttpStatusCode.Forbidden, actual = response.status)
    }
}
