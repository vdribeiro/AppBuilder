package com.app.builder

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFStringCreateWithCString
import platform.CoreFoundation.CFStringRef
import platform.CoreFoundation.CFTypeRef
import platform.CoreFoundation.kCFStringEncodingUTF8
import platform.Foundation.NSData
import platform.Foundation.create
import platform.posix.memcpy

/**
 * Copies this data into a new [ByteArray].
 *
 * @return A byte array with the same contents as this data.
 */
fun NSData.toByteArray(): ByteArray = ByteArray(size = length.toInt()).apply {
    if (isNotEmpty()) usePinned { pinned -> memcpy(__dst = pinned.addressOf(index = 0), __src = bytes, __n = length) }
}

/**
 * Copies this array into a new [NSData].
 *
 * @return An [NSData] with the same contents as this array.
 */
fun ByteArray.toNSData(): NSData =
    if (isEmpty()) NSData() else usePinned { pinned -> NSData.create(bytes = pinned.addressOf(index = 0), length = size.toULong()) }

/**
 * Formats this byte array as an uppercase hex string.
 *
 * @return The uppercase hex representation of this byte array.
 */
fun ByteArray.toHex(): String = joinToString(separator = "") { byte ->
    (byte.toInt() and 0xFF).toString(radix = 16).padStart(length = 2, padChar = '0')
}.uppercase()

/**
 * Converts this string to a Core Foundation string.
 *
 * @receiver The string to convert.
 * @return The Core Foundation string, or null if it could not be created.
 */
fun String.toCFString(): CFStringRef? = CFStringCreateWithCString(alloc = null, cStr = this, encoding = kCFStringEncodingUTF8)

/**
 * Runs [block] with this Core Foundation reference and releases it afterward via [CFRelease].
 *
 * @receiver The Core Foundation reference to release after use.
 * @param block The operation to perform with the reference.
 * @return The result of [block].
 */
inline fun <T: CFTypeRef?, R> T.use(block: (T) -> R): R {
    try {
        return block(this)
    } finally {
        if (this != null) CFRelease(cf = this)
    }
}
