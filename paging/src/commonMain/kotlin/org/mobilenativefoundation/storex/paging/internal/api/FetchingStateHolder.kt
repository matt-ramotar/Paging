package org.mobilenativefoundation.storex.paging.internal.api

import kotlinx.coroutines.flow.StateFlow
import org.mobilenativefoundation.storex.paging.Identifier

interface FetchingStateHolder<Id : Identifier<*>, K : Any> {
    val state: StateFlow<FetchingState<Id, K>>

    fun update(reducer: (prevState: FetchingState<Id, K>) -> FetchingState<Id, K>)
    fun update(nextState: FetchingState<Id, K>)
    fun updateMaxItemAccessedSoFar(id: Id)
    fun updateMinItemAccessedSoFar(id: Id)
    fun updateMaxRequestSoFar(key: K)
    fun updateMinRequestSoFar(key: K)
    fun updateMinItemLoadedSoFar(id: Id)
    fun updateMaxItemLoadedSoFar(id: Id)
}