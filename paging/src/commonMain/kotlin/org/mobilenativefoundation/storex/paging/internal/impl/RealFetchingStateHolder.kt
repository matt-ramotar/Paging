package org.mobilenativefoundation.storex.paging.internal.impl

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.mobilenativefoundation.storex.paging.Identifier
import org.mobilenativefoundation.storex.paging.internal.api.FetchingState
import org.mobilenativefoundation.storex.paging.internal.api.FetchingStateHolder

/**
 * TODO(): Make this thread safe!
 */
class RealFetchingStateHolder<Id : Identifier<Id>, K : Comparable<K>>(
    initialFetchingState: FetchingState<Id, K> = FetchingState()
) : FetchingStateHolder<Id, K> {
    private val _state = MutableStateFlow(initialFetchingState)
    override val state: StateFlow<FetchingState<Id, K>> = _state.asStateFlow()
    override fun updateMaxItemLoadedSoFar(id: Id) {
        val maxSoFar = _state.value.maxItemLoadedSoFar?.let {

            if (it > id) it else id
        } ?: id

        _state.value = _state.value.copy(maxItemLoadedSoFar = maxSoFar)
    }

    override fun updateMinItemLoadedSoFar(id: Id) {
        val minSoFar = _state.value.minItemLoadedSoFar?.let {
            if (it < id) it else id
        } ?: id

        _state.value = _state.value.copy(minItemLoadedSoFar = minSoFar)
    }

    override fun updateMinRequestSoFar(key: K) {
        val minRequestSoFar = _state.value.minRequestSoFar?.let {
            minOf(it, key)
        } ?: key

        _state.value = _state.value.copy(minRequestSoFar = minRequestSoFar)
    }

    override fun updateMaxRequestSoFar(key: K) {
        val maxRequestSoFar = _state.value.maxRequestSoFar?.let {
            maxOf(key, it)
        } ?: key

        _state.value = _state.value.copy(maxRequestSoFar = maxRequestSoFar)
    }

    override fun updateMinItemAccessedSoFar(id: Id) {
        val minItemAccessedSoFar = _state.value.minItemAccessedSoFar?.let {
            if (it < id) it else id
        } ?: id

        _state.value = _state.value.copy(minItemAccessedSoFar = minItemAccessedSoFar)
    }

    override fun updateMaxItemAccessedSoFar(id: Id) {
        val maxItemAccessedSoFar = _state.value.maxItemAccessedSoFar?.let {
            if (it > id) it else id
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