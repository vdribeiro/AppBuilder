package com.app.builder.core.flow

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import com.app.builder.test.TestCase

class LazyDataTest: TestCase() {

    /** Verifies the first get call invokes load and returns its value. */
    @Test
    fun getInvokesLoadOnFirstCall() = runUnitTest {
        var loadCount = 0
        val lazyData = LazyData { loadCount++; "value" }

        assertEquals(expected = "value", actual = lazyData.get())
        assertEquals(expected = 1, actual = loadCount)
    }

    /** Verifies a second get call returns the cached value without invoking load again. */
    @Test
    fun getReturnsCachedValueOnSubsequentCalls() = runUnitTest {
        var loadCount = 0
        val lazyData = LazyData { loadCount++; "value" }

        assertEquals(expected = "value", actual = lazyData.get())
        assertEquals(expected = "value", actual = lazyData.get())
        assertEquals(expected = "value", actual = lazyData.get())
        assertEquals(expected = 1, actual = loadCount)
    }

    /** Verifies concurrent get calls only invoke load once, guarded by the internal mutex. */
    @Test
    fun concurrentGetCallsOnlyInvokeLoadOnce() = runUnitTest {
        var loadCount = 0
        val lazyData = LazyData<String> {
            loadCount++
            delay(timeMillis = 10)
            "value"
        }

        val results = coroutineScope {
            List(size = 20) { async { lazyData.get() } }.awaitAll()
        }

        assertEquals(expected = 1, actual = loadCount)
        assertTrue(actual = results.all { it == "value" })
    }
}
