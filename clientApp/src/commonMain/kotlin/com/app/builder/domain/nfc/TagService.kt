package com.app.builder.domain.nfc

/** Tag service that listens for NFC tag reads. */
interface TagService {

    /** Stops the service, suspending until all jobs have fully canceled. */
    suspend fun stop()

    /**
     * Starts the service.
     * If the service is already running, this is a no-op.
     */
    fun start()
}
