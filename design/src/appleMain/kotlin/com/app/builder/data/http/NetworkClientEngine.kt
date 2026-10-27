package com.app.builder.data.http

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin

actual fun createNetworkEngine(): HttpClientEngine = Darwin.create()
