package com.app.builder.domain.storage

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.uuid.Uuid
import com.app.builder.core.security.uuid
import com.app.builder.test.TestCase

class AvatarKeyTest: TestCase() {

    /** Verifies a freshly created key is one its own user may claim. */
    @Test
    fun createdKeyBelongsToItsUser() = runUnitTest {
        val userUuid = uuid()
        assertTrue(actual = AvatarKey.belongsTo(key = AvatarKey.create(userUuid = userUuid), userUuid = userUuid))
    }

    /** Verifies each upload gets its own key, so a stored URL never changes meaning. */
    @Test
    fun createdKeysAreUnique() = runUnitTest {
        val userUuid = uuid()
        assertTrue(actual = AvatarKey.create(userUuid = userUuid) != AvatarKey.create(userUuid = userUuid))
    }

    /** Verifies a user cannot claim an image uploaded into another user's folder. */
    @Test
    fun anotherUsersKeyIsRejected() = runUnitTest {
        val owner = uuid()
        val claimant = uuid()
        assertFalse(actual = AvatarKey.belongsTo(key = AvatarKey.create(userUuid = owner), userUuid = claimant))
    }

    /** Verifies keys outside the avatar folder are rejected, so the column cannot be pointed at an unrelated object. */
    @Test
    fun keysOutsideTheAvatarFolderAreRejected() = runUnitTest {
        val userUuid = uuid()
        listOf(
            "$userUuid/image.jpg",
            "secrets/$userUuid/image.jpg",
            "avatars/image.jpg",
            "",
            "avatars/$userUuid"
        ).forEach { key ->
            assertFalse(actual = AvatarKey.belongsTo(key = key, userUuid = userUuid), message = "accepted $key")
        }
    }

    /** Verifies a key cannot escape its own folder by nesting further paths under it. */
    @Test
    fun nestedKeysAreRejected() = runUnitTest {
        val userUuid = uuid()
        val other = uuid()
        assertFalse(actual = AvatarKey.belongsTo(key = "avatars/$userUuid/$other/image.jpg", userUuid = userUuid))
    }

    /** Verifies a prefix that merely starts with the user's uuid is not mistaken for that user's folder. */
    @Test
    fun aLongerUuidPrefixIsRejected() = runUnitTest {
        val userUuid = Uuid.parse(uuidString = "00000000-0000-0000-0000-000000000000")
        assertFalse(actual = AvatarKey.belongsTo(key = "avatars/00000000-0000-0000-0000-000000000000x/image.jpg", userUuid = userUuid))
    }
}
