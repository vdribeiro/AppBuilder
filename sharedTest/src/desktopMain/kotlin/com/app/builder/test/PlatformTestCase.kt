package com.app.builder.test

actual abstract class PlatformTestCase actual constructor(): SharedTestCase() {

    /** Installs an empty [FakeDesktopKeyStore], since tests run without access to the keystore file and the operating system's secret store. */
    init {
        FakeDesktopKeyStore.install()
    }
}
