package com.app.builder.core.nfc

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import android.app.Activity
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import com.app.builder.NfcInfo
import com.app.builder.applicationContext
import com.app.builder.core.config.ClientFlags
import com.app.builder.core.flow.Dispatcher
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.serializer.decode
import com.app.builder.data.serializer.encode
import com.app.builder.toHex

/** Android [NfcController] implementation backed by [NfcAdapter]. */
internal class AndroidNfcController: NfcController() {

    /** Scope for working with nfc tags. */
    private val scope = CoroutineScope(context = SupervisorJob())
    /** Guards against overlapping reads/writes racing each other against the same tag's hardware I/O channel. */
    private val mutex = Mutex()
    /** The device's NFC adapter, or null if the device has no NFC hardware. */
    private val adapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(applicationContext)
    /** The current foreground activity. */
    private var activity: Activity? = null
    /** Records queued for the next tag detected while [State.Writing]. */
    private var pendingRecords: List<NfcRecord>? = null

    override val available: Boolean = adapter != null && adapter.isEnabled

    override fun platformDispose() {
        super.platformDispose()
        activity = null
        pendingRecords = null
    }

    override fun platformStop() {
        super.platformStop()
        pendingRecords = null
        val activity = this.activity ?: error(message = "No activity")
        val nfcAdapter = adapter ?: error(message = "No adapter")
        nfcAdapter.disableReaderMode(activity)
    }

    override fun platformRead() {
        super.platformRead()
        pendingRecords = null
        scan()
    }

    override fun platformWrite(records: List<NfcRecord>) {
        super.platformWrite(records = records)
        pendingRecords = records
        scan()
    }

    /** Enables reader mode against the current foreground activity, targeting NDEF-capable tag technologies. */
    private fun scan() {
        val activity = this.activity ?: error(message = "No activity")
        val nfcAdapter = adapter ?: error(message = "No adapter")
        val flags = NfcAdapter.FLAG_READER_NFC_A or
                NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_NFC_F or
                NfcAdapter.FLAG_READER_NFC_V or
                NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK
        val readerCallback = NfcAdapter.ReaderCallback { tag -> handleTag(tag = tag) }
        nfcAdapter.enableReaderMode(activity, readerCallback, flags, null)
    }

    /**
     * Routes a newly discovered [tag] to the read or write path.
     *
     * @param tag The tag delivered by the foreground dispatch intent.
     */
    private fun handleTag(tag: Tag) {
        when (state.value) {
            State.Scanning -> readTag(tag = tag)
            State.Writing -> writeTag(tag = tag)
            State.Idle -> stop()
        }
    }

    /**
     * Reads [tag]'s NDEF content.
     *
     * @param tag The tag to read.
     */
    private fun readTag(tag: Tag) {
        if (!mutex.tryLock()) return
        scope.launch(context = Dispatcher.IO) {
            try {
                TagProvider.sendTag(tag = tag.toNfcTag())
            } catch (throwable: Throwable) {
                Telemetry.error(tag = TAG, message = "Unable to read tag", throwable = throwable)
            } finally {
                stop()
                mutex.unlock()
            }
        }
    }

    /**
     * Writes [pendingRecords] to [tag], formatting it first if it isn't already NDEF-formatted.
     *
     * @param tag The tag to write to.
     */
    private fun writeTag(tag: Tag) {
        if (!mutex.tryLock()) return
        scope.launch(context = Dispatcher.IO) {
            try {
                val records = pendingRecords ?: error(message = "No pending records to write")
                val message = NdefMessage(records.mapNotNull { it.toNdefRecord() }.toTypedArray())
                when (val ndef = Ndef.get(tag)) {
                    null -> NdefFormatable.get(tag)?.use {
                        it.connect()
                        it.format(message)
                    } ?: error(message = "Tag does not support NDEF")

                    else -> ndef.use {
                        it.connect()
                        check(value = it.isWritable) { "Tag is read-only" }
                        check(value = message.toByteArray().size <= it.maxSize) { "NDEF message is too large for this tag" }
                        it.writeNdefMessage(message)
                    }
                }
            } catch (throwable: Throwable) {
                Telemetry.error(tag = TAG, message = "Unable to write tag", throwable = throwable)
            } finally {
                stop()
                mutex.unlock()
            }
        }
    }

    /**
     * Attaches or detaches the foreground [Activity] hosting this [NfcController].
     *
     * @param activity The current foreground activity, or null to detach.
     */
    internal fun set(activity: Activity?) {
        this.activity = activity
    }

    /** Stop scan while the host activity is not in the foreground. */
    internal fun onBackground() {
        runCatching {
            val activity = this.activity ?: error(message = "No activity")
            val nfcAdapter = adapter ?: error(message = "No adapter")
            nfcAdapter.disableReaderMode(activity)
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to pause NFC scan", throwable = it)
        }
    }

    /** Resume scan so foreground taps are handled in-app instead of falling through to the system's NFC tag dispatch/notification. */
    internal fun onForeground() {
        runCatching {
            if (!ClientFlags.flags.nfc || !available) return@runCatching stop()
            scan()
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to resume NFC scan", throwable = it)
        }
    }

    /**
     * Converts an [NfcRecord] to a platform [NdefRecord].
     *
     * @return The corresponding [NdefRecord], or null on error.
     */
    private fun NfcRecord.toNdefRecord(): NdefRecord? = runCatching {
        val json = encode(value = this) ?: error(message = "Unable to encode NfcRecord")
        NdefRecord.createMime(
            NfcInfo.NFC_ACTION_MIME_TYPE,
            json.encodeToByteArray()
        )
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable parse record", throwable = it)
    }.getOrNull()

    companion object {
        private const val TAG = "AndroidNfcController"

        /**
         * Reads this [Tag]'s NDEF content into an [NfcTag].
         *
         * @return The decoded [NfcTag].
         */
        internal suspend fun Tag.toNfcTag(): NfcTag = withContext(context = Dispatcher.IO) {
            val records = when (val ndef = Ndef.get(this@toNfcTag)) {
                null -> emptyList()
                else -> ndef.use {
                    it.connect()
                    it.ndefMessage?.records?.mapNotNull { record -> record.toNfcRecord() } ?: emptyList()
                }
            }
            NfcTag(id = id.toHex(), records = records)
        }

        /**
         * Converts a NDEF record to a [NfcRecord], if it carries this app's mime type and decodes successfully.
         *
         * @return The decoded [NfcRecord], or null if this isn't a matching/valid record.
         */
        private fun NdefRecord.toNfcRecord(): NfcRecord? = runCatching {
            if (tnf != NdefRecord.TNF_MIME_MEDIA) return null
            if (String(bytes = type, charset = Charsets.US_ASCII) != NfcInfo.NFC_ACTION_MIME_TYPE) return null
            return decode<NfcRecord>(value = payload.decodeToString())
        }.onFailure {
            Telemetry.error(tag = "AndroidNfc", message = "Unable parse payload", throwable = it)
        }.getOrNull()
    }
}

internal actual fun createNfcController(): NfcController = AndroidNfcController()
