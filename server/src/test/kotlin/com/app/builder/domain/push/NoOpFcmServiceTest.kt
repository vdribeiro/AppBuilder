package com.app.builder.domain.push

import kotlin.test.Test
import kotlin.test.assertTrue
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class NoOpFcmServiceTest: TestCase() {

    /** Verifies that every delivery method reports success and no failed tokens. */
    @Test
    fun everyDeliveryReportsSuccessWithoutFailedTokens() = runServerTest {
        assertTrue(actual = NoOpFcmService.subscribeToBroadcastTopic(token = "token"))
        assertTrue(actual = NoOpFcmService.sendToBroadcastTopic(pushPayload = FakeData.notification))
        assertTrue(actual = NoOpFcmService.sendToTokens(tokens = listOf("token"), pushPayload = FakeData.notification).isEmpty())
    }
}
