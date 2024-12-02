package com.app.builder.core.nfc

import kotlinx.cinterop.ObjCSignatureOverride
import platform.CoreNFC.NFCFeliCaTagProtocol
import platform.CoreNFC.NFCISO15693TagProtocol
import platform.CoreNFC.NFCISO7816TagProtocol
import platform.CoreNFC.NFCMiFareTagProtocol
import platform.CoreNFC.NFCNDEFMessage
import platform.CoreNFC.NFCNDEFPayload
import platform.CoreNFC.NFCNDEFReaderSession
import platform.CoreNFC.NFCNDEFReaderSessionDelegateProtocol
import platform.CoreNFC.NFCNDEFTagProtocol
import platform.CoreNFC.NFCTypeNameFormatMedia
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.darwin.NSObject
import com.app.builder.NfcInfo
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.serializer.decode
import com.app.builder.data.serializer.encode
import com.app.builder.toByteArray
import com.app.builder.toHex
import com.app.builder.toNSData

/** Apple [NfcController] implementation backed by Core NFC's [NFCNDEFReaderSession]. */
internal class AppleNfcController: NfcController() {

    /** The active reader session, while a scan or write is in progress. */
    private var session: NFCNDEFReaderSession? = null
    /** The delegate backing [session], kept alive for the session's lifetime since [NFCNDEFReaderSession.delegate] is a weak reference. */
    private var delegate: ReaderDelegate? = null
    /** Records queued for the next tag detected while [State.Writing]. */
    private var pendingRecords: List<NfcRecord>? = null

    override val available: Boolean = NFCNDEFReaderSession.readingAvailable

    override fun platformDispose() {
        super.platformDispose()
        session = null
        delegate = null
        pendingRecords = null
    }

    override fun platformStop() {
        super.platformStop()
        session?.invalidateSession()
        session = null
        delegate = null
        pendingRecords = null
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

    /** Begins a new [NFCNDEFReaderSession], replacing [session]/[delegate] with the new instances. */
    private fun scan() {
        val newDelegate = ReaderDelegate(onTag = ::handleTag)
        val newSession = NFCNDEFReaderSession(
            delegate = newDelegate,
            queue = null,
            invalidateAfterFirstRead = false
        ).apply {
            alertMessage = "scan_nfc_tag"
            beginSession()
        }
        session = newSession
        delegate = newDelegate
    }

    /**
     * Routes a detected tag to [readTag] or [writeTag] depending on the current [State], or invalidates [session] if neither is active.
     *
     * @param tag The detected tag.
     * @param session The active reader session.
     */
    private fun handleTag(tag: NFCNDEFTagProtocol, session: NFCNDEFReaderSession) {
        when (state.value) {
            State.Scanning -> readTag(tag = tag, session = session)
            State.Writing -> writeTag(tag = tag, session = session)
            State.Idle -> stop()
        }
    }

    /**
     * Connects to [tag] and reads its NDEF message.
     *
     * @param tag The tag to read.
     * @param session The active reader session.
     */
    private fun readTag(tag: NFCNDEFTagProtocol, session: NFCNDEFReaderSession) {
        session.connectToTag(tag = tag) { connectError ->
            if (connectError != null) {
                invalidateSession(session = session, message = "Unable to connect to tag", error = connectError)
                return@connectToTag
            }

            tag.readNDEFWithCompletionHandler { message, readError ->
                if (readError != null) {
                    invalidateSession(session = session, message = "Unable to read tag", error = readError)
                    return@readNDEFWithCompletionHandler
                }

                val records = message?.records?.mapNotNull { record -> (record as? NFCNDEFPayload)?.toNfcRecord() } ?: emptyList()
                TagProvider.sendTag(tag = NfcTag(id = tag.serialNumber(), records = records))

                session.alertMessage = "tag_read"
                session.invalidateSession()
                stop()
            }
        }
    }

    /**
     * Connects to [tag] and writes [pendingRecords] to it.
     *
     * @param tag The tag to write to.
     * @param session The active reader session.
     */
    private fun writeTag(tag: NFCNDEFTagProtocol, session: NFCNDEFReaderSession) {
        val records = pendingRecords ?: run {
            invalidateSession(session = session, message = "No pending records")
            return
        }

        session.connectToTag(tag = tag) { connectError ->
            if (connectError != null) {
                invalidateSession(session = session, message = "Unable to connect to tag", error = connectError)
                return@connectToTag
            }

            val message = NFCNDEFMessage(nDEFRecords = records.mapNotNull { it.toNdefPayload() })
            tag.writeNDEF(ndefMessage = message) { writeError ->
                if (writeError != null) {
                    invalidateSession(session = session, message = "Unable to write tag", error = writeError)
                    return@writeNDEF
                }

                session.alertMessage = "tag_write"
                session.invalidateSession()
                stop()
            }
        }
    }

    /**
     * Logs [error] under [message], invalidates [session] with the same message shown to the user, and returns to [State.Idle].
     *
     * @param session The session to invalidate.
     * @param message Shown to the user and logged alongside [error].
     * @param error The underlying Core NFC error.
     */
    private fun invalidateSession(session: NFCNDEFReaderSession, message: String, error: NSError? = null) {
        Telemetry.error(tag = TAG, message = message, throwable = error?.let { Throwable(message = it.localizedDescription) })
        session.invalidateSessionWithErrorMessage(errorMessage = message)
        stop()
    }

    /**
     * Core NFC reader-session delegate.
     *
     * @property onTag Called with the detected tag and its session whenever [readerSession] fires.
     */
    private class ReaderDelegate(
        private val onTag: (NFCNDEFTagProtocol, NFCNDEFReaderSession) -> Unit,
    ): NSObject(), NFCNDEFReaderSessionDelegateProtocol {

        @ObjCSignatureOverride
        override fun readerSession(session: NFCNDEFReaderSession, didDetectTags: List<*>) {
            (didDetectTags.firstOrNull() as? NFCNDEFTagProtocol)?.let { onTag(it, session) }
        }

        @ObjCSignatureOverride
        override fun readerSession(session: NFCNDEFReaderSession, didDetectNDEFs: List<*>) {
        }

        override fun readerSession(session: NFCNDEFReaderSession, didInvalidateWithError: NSError) {
            Telemetry.error(tag = TAG, message = "NFC session invalidated: ${didInvalidateWithError.localizedDescription}")
        }
    }

    /**
     * Extracts this tag's serial number by checking each concrete Core NFC tag subtype in turn since [NFCNDEFTagProtocol] itself exposes no generic accessor.
     *
     * @return The identifier or an empty string if the concrete type isn't recognized, or exposes no serial number.
     */
    private fun NFCNDEFTagProtocol.serialNumber(): String {
        val identifier = when (this) {
            is NFCMiFareTagProtocol -> identifier
            is NFCISO7816TagProtocol -> identifier
            is NFCISO15693TagProtocol -> identifier
            is NFCFeliCaTagProtocol -> currentIDm
            else -> null
        }
        return identifier?.toByteArray()?.toHex().orEmpty()
    }

    /**
     * Converts a Core NFC [NFCNDEFPayload] read from a tag to an [NfcRecord].
     *
     * @return The decoded [NfcRecord], or null if this isn't a matching/valid record.
     */
    private fun NFCNDEFPayload.toNfcRecord(): NfcRecord? = runCatching {
        if (typeNameFormat != NFCTypeNameFormatMedia) return null
        if (type.toByteArray().decodeToString() != NfcInfo.NFC_ACTION_MIME_TYPE) return null
        decode<NfcRecord>(value = payload.toByteArray().decodeToString())
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable parse payload", throwable = it)
    }.getOrNull()

    /**
     * Converts an [NfcRecord] to a Core NFC [NFCNDEFPayload].
     *
     * @return The converted [NFCNDEFPayload], or null on error.
     */
    private fun NfcRecord.toNdefPayload(): NFCNDEFPayload? = runCatching {
        val json = encode(value = this) ?: error(message = "Unable to encode NfcAction")
        NFCNDEFPayload(
            format = NFCTypeNameFormatMedia,
            type = NfcInfo.NFC_ACTION_MIME_TYPE.encodeToByteArray().toNSData(),
            identifier = NSData(),
            payload = json.encodeToByteArray().toNSData(),
        )
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable parse record", throwable = it)
    }.getOrNull()

    companion object {
        private const val TAG = "AppleNfcController"
    }
}

internal actual fun createNfcController(): NfcController = AppleNfcController()
