package org.mobilenativefoundation.storex.paging.runtime

import kotlinx.coroutines.flow.StateFlow

interface Pager<ItemId : Any, PageRequestKey : Any, ItemValue : Any> {
    val state: StateFlow<PagingState<ItemId, PageRequestKey, ItemValue>>
}
