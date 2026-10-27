package com.app.builder.data.http

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp

actual fun createNetworkEngine(): HttpClientEngine = OkHttp.create()
