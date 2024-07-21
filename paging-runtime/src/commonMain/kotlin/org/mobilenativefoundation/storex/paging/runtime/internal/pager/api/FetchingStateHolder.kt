package org.mobilenativefoundation.storex.paging.runtime.internal.pager.api

import kotlinx.coroutines.flow.StateFlow
import org.mobilenativefoundation.storex.paging.runtime.FetchingState

interface FetchingStateHolder<ItemId: Any, PageRequestKey : Any> {
    val state: StateFlow<FetchingState<ItemId, PageRequestKey>>

    suspend fun update(reducer: (prevState: FetchingState<ItemId, PageRequestKey>) -> FetchingState<ItemId, PageRequestKey>)
    suspend fun update(nextState: FetchingState<ItemId, PageRequestKey>)
    suspend fun updateMaxItemAccessedSoFar(id: ItemId)
    suspend fun updateMinItemAccessedSoFar(id: ItemId)
    suspend fun updateMaxRequestSoFar(key: PageRequestKey)
    suspend fun updateMinRequestSoFar(key: PageRequestKey)
    suspend fun updateMinItemLoadedSoFar(id: ItemId)
    suspend fun updateMaxItemLoadedSoFar(id: ItemId)
}