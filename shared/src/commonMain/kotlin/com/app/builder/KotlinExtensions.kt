package com.app.builder

/**
 * Returns the enum entry of type [T] matching this string, or `null` if no match is found.
 * Matching is case-insensitive and automatically handles spaces by converting them to underscores.
 *
 * @param T The type of the enum.
 * @return The enum entry of type [T] matching this string, or `null` if no match is found.
 */
inline fun <reified T: Enum<T>> String.toEnumOrNull(): T? =
    enumValues<T>().firstOrNull { it.name.equals(other = replace(oldChar = ' ', newChar = '_'), ignoreCase = true) }

/**
 * Adds [element] if it is not present, otherwise removes it.
 *
 * @param element Element to add or remove.
 * @return The changed [Iterable].
 */
fun <V> Iterable<V>.plusOrMinus(element: V): Collection<V> =
    if (contains(element = element)) minus(element = element) else plus(element = element)
