package org.mobilenativefoundation.storex.paging.internal.api

import kotlinx.coroutines.flow.StateFlow
import org.mobilenativefoundation.storex.paging.Quantifiable

interface FetchingStateHolder<Id : Comparable<Id>, Q: Quantifiable<Id>, K : Any> {
    val state: StateFlow<FetchingState<Id, Q, K>>

    fun update(reducer: (prevState: FetchingState<Id, Q, K>) -> FetchingState<Id, Q, K>)
    fun update(nextState: FetchingState<Id, Q, K>)
    fun updateMaxItemAccessedSoFar(id: Q)
    fun updateMinItemAccessedSoFar(id: Q)
    fun updateMaxRequestSoFar(key: K)
    fun updateMinRequestSoFar(key: K)
}