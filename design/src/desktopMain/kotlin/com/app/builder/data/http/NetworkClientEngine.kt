package com.app.builder.data.http

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO

actual fun createNetworkEngine(): HttpClientEngine = CIO.create()
