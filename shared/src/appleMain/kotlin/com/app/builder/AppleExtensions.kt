package com.app.builder

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
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