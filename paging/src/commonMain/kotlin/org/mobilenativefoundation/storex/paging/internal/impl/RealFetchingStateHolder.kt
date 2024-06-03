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
class RealFetchingStateHolder<Id : Comparable<Id>, Q : Quantifiable<Id>, K : Comparable<K>>(
    initialFetchingState: FetchingState<Id, Q, K> = FetchingState()
) : FetchingStateHolder<Id, Q, K> {
    private val _state = MutableStateFlow(initialFetchingState)
    override val state: StateFlow<FetchingState<Id, Q, K>> = _state.asStateFlow()
    override fun updateMinRequestSoFar(key: K) {
        val minRequestSoFar = _state.value.minRequestSoFar?.let {
            minOf(it, key)
        } ?: key

        _state.value = _state.value.copy(minRequestSoFar = minRequestSoFar)
    }

    override fun updateMaxRequestSoFar(key: K) {
        val maxRequestSoFar = _state.value.maxRequestSoFar?.let {
            maxOf(it, key)
        } ?: key

        _state.value = _state.value.copy(maxRequestSoFar = maxRequestSoFar)
    }

    override fun updateMinItemAccessedSoFar(id: Q) {
        val minItemAccessedSoFar = _state.value.minItemAccessedSoFar?.let {
            if (it.value < id.value) it else id
        } ?: id

        _state.value = _state.value.copy(minItemAccessedSoFar = minItemAccessedSoFar)
    }

    override fun updateMaxItemAccessedSoFar(id: Q) {
        val maxItemAccessedSoFar = _state.value.maxItemAccessedSoFar?.let {
            if (it.value > id.value) it else id
        } ?: id
        println("UPDATING MAX ITEM ACCESSED SO FAR TO $maxItemAccessedSoFar")
        _state.value = _state.value.copy(maxItemAccessedSoFar = maxItemAccessedSoFar)
    }

    override fun update(nextState: FetchingState<Id, Q, K>) {
        _state.value = nextState
    }

    override fun update(reducer: (prevState: FetchingState<Id, Q, K>) -> FetchingState<Id, Q, K>) {
        _state.value = reducer(_state.value)
    }

}