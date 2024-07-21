package org.mobilenativefoundation.storex.paging.runtime

interface Dispatcher<ItemId : Any, PageRequestKey : Any, ItemValue : Any> {
    suspend fun dispatch(action: PagingAction<ItemId, PageRequestKey, ItemValue>)
}