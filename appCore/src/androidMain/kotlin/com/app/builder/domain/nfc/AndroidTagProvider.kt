package com.app.builder.domain.nfc

import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import androidx.core.content.IntentCompat
import com.app.builder.core.nfc.AndroidNfcController.Companion.toNfcTag
import com.app.builder.core.nfc.TagProvider
import com.app.builder.core.telemetry.Telemetry

object AndroidTagProvider {

    private const val TAG = "AndroidTagProvider"

    /**
     * Extracts and handles a nfc tag from the intent's extras.
     *
     * @param intent The intent that started or was redelivered to the activity.
     * @return true in tag was read, false otherwise.
     */
    suspend fun handleNfcTag(intent: Intent): Boolean = runCatching {
        if (intent.action !in setOf(NfcAdapter.ACTION_TECH_DISCOVERED, NfcAdapter.ACTION_NDEF_DISCOVERED)) error(message = "Wrong intent")
        val tag = IntentCompat.getParcelableExtra(intent, NfcAdapter.EXTRA_TAG, Tag::class.java) ?: error(message = "No tag")
        TagProvider.sendTag(tag = tag.toNfcTag())
        true
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable to handle payload from intent", throwable = it)
    }.getOrDefault(defaultValue = false)
}