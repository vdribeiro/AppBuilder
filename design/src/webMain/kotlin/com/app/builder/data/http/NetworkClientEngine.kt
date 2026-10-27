package com.app.builder.data.http

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.js.Js

actual fun createNetworkEngine(): HttpClientEngine = Js.create()
