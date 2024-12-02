package com.app.builder.core.flow

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

actual val io: CoroutineDispatcher = Dispatchers.Default
