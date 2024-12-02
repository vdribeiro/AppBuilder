package com.app.builder

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import android.content.Context
import android.content.ContextWrapper
import androidx.test.core.app.ApplicationProvider
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class KInitializerTest {

    /** Resets the application context before each test. */
    @Before
    fun setup() {
        applicationContext = ContextWrapper(null)
    }

    /** Verifies that creating the initializer sets the application context. */
    @Test
    fun initializer() {
        val context: Context = ApplicationProvider.getApplicationContext()
        val initializer = KInitializer()
        initializer.create(context = context)
        assertEquals(expected = context, actual = applicationContext)
    }

    /** Verifies that [KInitializer] declares no dependencies. */
    @Test
    fun dependencies() {
        val initializer = KInitializer()
        assertTrue(actual = initializer.dependencies().isEmpty())
    }
}
