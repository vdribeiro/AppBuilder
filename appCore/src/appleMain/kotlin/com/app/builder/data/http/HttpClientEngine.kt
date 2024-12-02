package com.app.builder.data.http

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import com.app.builder.test.ExcludeFromTesting

@ExcludeFromTesting
actual fun createHttpEngine(): HttpClientEngine = Darwin.create()
