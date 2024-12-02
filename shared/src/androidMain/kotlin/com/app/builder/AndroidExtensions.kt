package com.app.builder

/**
 * Formats this byte array as an uppercase hex string.
 *
 * @return The string hexadecimal.
 */
fun ByteArray.toHex(): String = joinToString(separator = "") { "%02X".format(it) }
