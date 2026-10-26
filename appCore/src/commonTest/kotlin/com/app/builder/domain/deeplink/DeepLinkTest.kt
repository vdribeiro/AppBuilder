package com.app.builder.domain.deeplink

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.app.builder.core.flow.Dispatcher
import com.app.builder.domain.EntityType
import com.app.builder.test.TestCase

class DeepLinkTest: TestCase() {

    /** Verifies that a sent deep link is delivered through [DeepLink.deepLinks]. */
    @Test
    fun sendDeliversDeepLinkToObservers() = runUnitTest {
        val deepLink = DeepLink.Entity(entityUuid = Uuid.random(), entityType = EntityType.TASK)

        DeepLink.send(deepLink = deepLink)

        assertEquals(expected = deepLink, actual = DeepLink.deepLinks.first())
    }

    /** Verifies that sending a new deep link replaces a buffered but uncollected one, since the channel is conflated. */
    @Test
    fun sendReplacesUnconsumedBufferedDeepLink() = runUnitTest {
        val first = DeepLink.Entity(entityUuid = Uuid.random(), entityType = EntityType.TASK)
        val second = DeepLink.Entity(entityUuid = Uuid.random(), entityType = EntityType.USER)

        DeepLink.send(deepLink = first)
        DeepLink.send(deepLink = second)

        assertEquals(expected = second, actual = DeepLink.deepLinks.first())
    }

    /** Verifies that [DeepLink.reset] discards any deep link buffered but not yet collected. */
    @Test
    fun resetDiscardsBufferedDeepLink() = runUnitTest {
        DeepLink.send(deepLink = DeepLink.Entity(entityUuid = Uuid.random(), entityType = EntityType.TASK))

        DeepLink.reset()

        val received = mutableListOf<DeepLink>()
        backgroundScope.launch(context = Dispatcher.Default) { DeepLink.deepLinks.collect { received.add(element = it) } }
        testScheduler.advanceUntilIdle()
        assertTrue(actual = received.isEmpty())
    }
}
