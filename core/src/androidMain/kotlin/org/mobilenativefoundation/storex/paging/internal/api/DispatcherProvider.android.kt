package org.mobilenativefoundation.storex.paging.internal.api

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

actual data object DispatcherProvider {
    actual val io: CoroutineDispatcher = Dispatchers.IO
}