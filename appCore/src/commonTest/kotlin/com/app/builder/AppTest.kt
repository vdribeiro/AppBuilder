package com.app.builder

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import com.app.builder.test.TestCase

class AppTest: TestCase() {

    /** The original app id, captured to restore it after each test. */
    private val id = App.id
    /** The original app name, captured to restore it after each test. */
    private val name = App.name
    /** The original app version, captured to restore it after each test. */
    private val version = App.version

    /** Restores the original app info after each test. */
    @AfterTest
    fun restoreInfo() = App.setInfo(id = id, name = name, version = version)

    /** Verifies that [App.setInfo] updates the app id, name and version. */
    @Test
    fun setInfo() = runUnitTest {
        App.setInfo(id = "awesome.app", name = "Awesome App", version = "1.2.3")

        assertEquals(expected = "awesome.app", actual = App.id)
        assertEquals(expected = "Awesome App", actual = App.name)
        assertEquals(expected = "1.2.3", actual = App.version)
    }
}
