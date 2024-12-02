package com.app.builder.data.http

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import com.app.builder.test.ExcludeFromTesting

@ExcludeFromTesting
actual fun createHttpEngine(): HttpClientEngine = OkHttp.create()
