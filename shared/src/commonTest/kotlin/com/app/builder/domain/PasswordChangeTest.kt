package com.app.builder.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import com.app.builder.data.serializer.decode
import com.app.builder.data.serializer.encode
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class PasswordChangeTest: TestCase() {

    /** Verifies toString masks the refresh token and both passwords. */
    @Test
    fun toStringMasksRefreshTokenAndPasswords() = runUnitTest {
        assertEquals(
            expected = "PasswordChange(refreshToken=********, currentPassword=********, newPassword=********)",
            actual = passwordChange.toString()
        )
    }

    /** Verifies a password change survives a JSON round trip with the raw refresh token and passwords intact. */
    @Test
    fun serializationRoundTrip() = runUnitTest {
        assertEquals(
            expected = passwordChange,
            actual = decode(value = assertNotNull(actual = encode(value = passwordChange)))
        )
    }

    companion object {
        private val passwordChange = PasswordChange(
            refreshToken = FakeData.bearerToken.refreshToken,
            currentPassword = FakeData.credentials.password,
            newPassword = "new_password"
        )
    }
}
