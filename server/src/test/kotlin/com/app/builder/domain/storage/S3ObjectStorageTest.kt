package com.app.builder.domain.storage

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import com.app.builder.core.locale.DateTime
import com.app.builder.test.TestCase

class S3ObjectStorageTest: TestCase() {

    /** Builds the storage under test against the documented example credentials. */
    private fun storage(): S3ObjectStorage = S3ObjectStorage(
        endpoint = ENDPOINT,
        bucket = BUCKET,
        region = REGION,
        accessKeyId = ACCESS_KEY_ID,
        secretAccessKey = SECRET_ACCESS_KEY
    )

    /**
     * Runs [block] with the clock fixed, so a signature scoped to a day and a second is reproducible.
     *
     * @param block The block to run.
     */
    private fun atFixedTime(block: () -> Unit) {
        val previous = DateTime.clock
        DateTime.clock = object: Clock {
            override fun now(): Instant = Instant.parse(input = SIGNED_AT)
        }
        try {
            block()
        } finally {
            DateTime.clock = previous
        }
    }

    /**
     * Verifies the signed upload URL matches, character for character, the one botocore produces for the same request.
     * A signature that is merely well formed is still rejected by the bucket, so the expected value is pinned against an independent implementation rather than against this one.
     */
    @Test
    fun presignUploadMatchesAReferenceSignature() = runUnitTest {
        atFixedTime {
            val url = storage().presignUpload(key = KEY, contentType = CONTENT_TYPE, expiry = 900.seconds)
            assertEquals(expected = REFERENCE_URL, actual = url)
        }
    }

    /** Verifies the signature binds the media type, so a granted upload cannot be reused to store something else. */
    @Test
    fun presignUploadSignsTheContentType() = runUnitTest {
        atFixedTime {
            val jpeg = storage().presignUpload(key = KEY, contentType = CONTENT_TYPE, expiry = 900.seconds)
            val png = storage().presignUpload(key = KEY, contentType = "image/png", expiry = 900.seconds)
            assertTrue(actual = jpeg != png, message = "the content type is not covered by the signature")
            assertTrue(actual = jpeg!!.contains(other = "X-Amz-SignedHeaders=content-type%3Bhost"))
        }
    }

    /** Verifies the expiry the caller asks for is the one carried in the URL. */
    @Test
    fun presignUploadCarriesTheRequestedExpiry() = runUnitTest {
        atFixedTime {
            val url = storage().presignUpload(key = KEY, contentType = CONTENT_TYPE, expiry = 120.seconds)
            assertTrue(actual = url!!.contains(other = "X-Amz-Expires=120"))
        }
    }

    /** Verifies an unconfigured bucket refuses to sign rather than handing out a URL that cannot work. */
    @Test
    fun noOpStorageSignsNothing() = runUnitTest {
        assertNull(actual = NoOpObjectStorage.presignUpload(key = KEY, contentType = CONTENT_TYPE, expiry = 900.seconds))
    }

    private companion object {
        private const val ENDPOINT = "https://br-little-leaf-b9rdsw92.storage.c-7.eu-central-1.aws.neon.tech"
        private const val BUCKET = "image"
        private const val REGION = "eu-central-1"
        private const val KEY = "avatars/aaa/bbb.jpg"
        private const val CONTENT_TYPE = "image/jpeg"

        /** The credentials AWS publishes for signature examples. They authorise nothing. */
        private const val ACCESS_KEY_ID = "AKIAIOSFODNN7EXAMPLE"
        private const val SECRET_ACCESS_KEY = "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY"

        private const val SIGNED_AT = "2026-03-04T05:06:07Z"

        /** Produced by botocore's `S3SigV4QueryAuth` for the same request, bucket, credentials and instant. */
        private const val REFERENCE_URL = "$ENDPOINT/$BUCKET/$KEY" +
            "?X-Amz-Algorithm=AWS4-HMAC-SHA256" +
            "&X-Amz-Credential=$ACCESS_KEY_ID%2F20260304%2F$REGION%2Fs3%2Faws4_request" +
            "&X-Amz-Date=20260304T050607Z" +
            "&X-Amz-Expires=900" +
            "&X-Amz-SignedHeaders=content-type%3Bhost" +
            "&X-Amz-Signature=b5496ceab11ead36f9b27b067a5a2761b2add4ce0001c9b9b3c46bc3a4216120"
    }
}
