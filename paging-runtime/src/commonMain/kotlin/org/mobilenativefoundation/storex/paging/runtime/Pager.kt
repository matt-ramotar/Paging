package org.mobilenativefoundation.storex.paging.runtime

import kotlinx.coroutines.flow.StateFlow

interface Pager<ItemId : Any> {
    val state: StateFlow<PagingState<ItemId>>
}
