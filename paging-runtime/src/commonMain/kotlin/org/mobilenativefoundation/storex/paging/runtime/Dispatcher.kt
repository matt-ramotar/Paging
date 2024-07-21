package org.mobilenativefoundation.storex.paging.runtime

interface Dispatcher<PageRequestKey: Any> {
    suspend fun dispatch(action: Action<PageRequestKey>)
}