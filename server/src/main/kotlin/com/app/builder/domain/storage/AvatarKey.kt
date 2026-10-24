package com.app.builder.domain.storage

import kotlin.uuid.Uuid
import com.app.builder.core.security.uuid

/** Builds and checks the object keys avatars are stored under, so a granted upload can only land in its own user's folder. */
object AvatarKey {

    /** The media type avatars are stored as, which every signed upload is bound to. */
    const val CONTENT_TYPE = "image/jpeg"

    /** The folder every avatar key starts with. */
    private const val PREFIX = "avatars"

    /** The extension matching [CONTENT_TYPE]. */
    private const val EXTENSION = "jpg"

    /**
     * Builds the key for a new avatar.
     * Each upload gets its own name so the resulting URL never changes meaning, which lets clients and caches treat it as permanent.
     *
     * @param userUuid The user the avatar belongs to.
     * @return The object key to store the image under.
     */
    fun create(userUuid: Uuid): String = "$PREFIX/$userUuid/${uuid()}.$EXTENSION"

    /**
     * Reports whether a key is one this user is allowed to claim, rejecting any attempt to point a user at another user's image or at an unrelated object.
     *
     * @param key The key being claimed.
     * @param userUuid The user claiming it.
     * @return `true` if the key sits in this user's folder.
     */
    fun belongsTo(key: String, userUuid: Uuid): Boolean =
        key.startsWith(prefix = "$PREFIX/$userUuid/") && !key.removePrefix(prefix = "$PREFIX/$userUuid/").contains(char = '/')
}
