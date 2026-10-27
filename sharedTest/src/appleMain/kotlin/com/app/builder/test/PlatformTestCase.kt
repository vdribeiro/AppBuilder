package com.app.builder.test

actual abstract class PlatformTestCase actual constructor(): SharedTestCase() {

    /** Installs an empty [FakeKeychain], since tests run without access to the Keychain. */
    init {
        FakeKeychain.install()
    }
}
