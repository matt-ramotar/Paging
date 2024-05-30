package org.mobilenativefoundation.storex.paging.internal.impl

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.mobilenativefoundation.storex.paging.Quantifiable
import org.mobilenativefoundation.storex.paging.internal.api.FetchingState
import org.mobilenativefoundation.storex.paging.internal.api.FetchingStateHolder

/**
 * TODO(): Make this thread safe!
 */
class RealFetchingStateHolder<Id : Comparable<Id>, K : Any>(
    initialFetchingState: FetchingState<Id, K> = FetchingState()
) : FetchingStateHolder<Id, K> {
    private val _state = MutableStateFlow(initialFetchingState)
    override val state: StateFlow<FetchingState<Id, K>> = _state.asStateFlow()
    override fun updateMinItemAccessedSoFar(id: Quantifiable<Id>) {
        TODO("Not yet implemented")
    }

    override fun updateMaxItemAccessedSoFar(id: Quantifiable<Id>) {
        val maxItemAccessedSoFar = _state.value.maxItemAccessedSoFar?.let {
            if (it.value > id.value) it else id
        } ?: id
        println("UPDATING MAX ITEM ACCESSED SO FAR TO $maxItemAccessedSoFar")
        _state.value = _state.value.copy(maxItemAccessedSoFar = maxItemAccessedSoFar)
    }

    override fun update(nextState: FetchingState<Id, K>) {
        _state.value = nextState
    }

    override fun update(reducer: (prevState: FetchingState<Id, K>) -> FetchingState<Id, K>) {
        _state.value = reducer(_state.value)
    }

}