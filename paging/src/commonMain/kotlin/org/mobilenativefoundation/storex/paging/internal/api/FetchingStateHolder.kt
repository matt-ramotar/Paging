package org.mobilenativefoundation.storex.paging.internal.api

import kotlinx.coroutines.flow.StateFlow
import org.mobilenativefoundation.storex.paging.Quantifiable

interface FetchingStateHolder<Id : Comparable<Id>, K : Any> {
    val state: StateFlow<FetchingState<Id, K>>

    fun update(reducer: (prevState: FetchingState<Id, K>) -> FetchingState<Id, K>)
    fun update(nextState: FetchingState<Id, K>)
    fun updateMaxItemAccessedSoFar(id: Quantifiable<Id>)
    fun updateMinItemAccessedSoFar(id: Quantifiable<Id>)
    fun updateMaxRequestSoFar(key: K)
    fun updateMinRequestSoFar(key: K)
}