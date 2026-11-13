package com.app.builder.test

/**
 * Makes the secret key fail to load, on the platforms whose key storage can be replaced.
 *
 * @return `true` when the secret key was made unavailable, or `false` on a platform whose key storage cannot be replaced.
 */
expect fun useUnavailableSecretKey(): Boolean
