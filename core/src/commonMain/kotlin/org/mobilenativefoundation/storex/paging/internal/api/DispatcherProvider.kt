package org.mobilenativefoundation.storex.paging.internal.api

import kotlinx.coroutines.CoroutineDispatcher

expect object DispatcherProvider {
    val io: CoroutineDispatcher
}