package org.mobilenativefoundation.storex.paging.internal.api

import kotlinx.coroutines.flow.StateFlow

interface FetchingStateHolder<Id : Comparable<Id>> {
    val state: StateFlow<FetchingState<Id>>

    fun update(reducer: (prevState: FetchingState<Id>) -> FetchingState<Id>)
    fun update(nextState: FetchingState<Id>)
    fun updateMaxItemAccessedSoFar(id: Id)
}