package com.app.builder.domain.translation

import kotlin.test.Test
import kotlin.test.assertNull
import kotlinx.coroutines.flow.firstOrNull
import com.app.builder.test.TestCase

class NoOpTranslationStoreTest: TestCase() {

    /** Verifies the store never emits translations and ignores sync calls. */
    @Test
    fun storeIsInert() = runUnitTest {
        val store = NoOpTranslationStore()
        assertNull(actual = store.observeTranslations(languageIso = "en").firstOrNull())
        store.syncTranslations()
        assertNull(actual = store.observeTranslations(languageIso = "en").firstOrNull())
    }
}
