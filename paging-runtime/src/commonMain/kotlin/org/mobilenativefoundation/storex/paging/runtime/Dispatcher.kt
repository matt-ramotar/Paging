package org.mobilenativefoundation.storex.paging.runtime

interface Dispatcher<ItemId : Any, PageRequestKey : Any, ItemValue : Any> {
    suspend fun dispatch(action: Action<ItemId, PageRequestKey, ItemValue>)
}