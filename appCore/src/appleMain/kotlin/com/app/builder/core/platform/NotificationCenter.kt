package com.app.builder.core.platform

import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.darwin.NSObjectProtocol

/**
 * Uses the notification dispatch mechanism to register an observer.
 *
 * @param name The name of the notification.
 * @param key The key of the notification.
 * @param onObserve The callback to be executed when the notification is observed.
 * @return The observer object.
 */
fun NSNotificationCenter.observe(
    name: String?,
    key: Any? = null,
    onObserve: () -> Unit
): NSObjectProtocol = addObserverForName(
    name = name,
    `object` = key,
    queue = NSOperationQueue.mainQueue
) { _ -> onObserve() }
