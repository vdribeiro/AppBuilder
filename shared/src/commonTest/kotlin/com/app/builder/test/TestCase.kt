package com.app.builder.test

/** Abstract class for defining test cases. It provides a hermetic testing environment. */
abstract class TestCase: PlatformTestCase() {
    override suspend fun beforeTest() {}
    override suspend fun afterTest() {}
}
