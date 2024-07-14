package org.mobilenativefoundation.storex.paging.runtime.internal.pager.api

import kotlinx.coroutines.flow.StateFlow
import org.mobilenativefoundation.storex.paging.runtime.FetchingState
import org.mobilenativefoundation.storex.paging.runtime.Identifier

interface FetchingStateHolder<Id : Identifier<*>, K : Any> {
    val state: StateFlow<FetchingState<Id, K>>

    suspend fun update(reducer: (prevState: FetchingState<Id, K>) -> FetchingState<Id, K>)
    suspend fun update(nextState: FetchingState<Id, K>)
    suspend fun updateMaxItemAccessedSoFar(id: Id)
    suspend fun updateMinItemAccessedSoFar(id: Id)
    suspend fun updateMaxRequestSoFar(key: K)
    suspend fun updateMinRequestSoFar(key: K)
    suspend fun updateMinItemLoadedSoFar(id: Id)
    suspend fun updateMaxItemLoadedSoFar(id: Id)
}