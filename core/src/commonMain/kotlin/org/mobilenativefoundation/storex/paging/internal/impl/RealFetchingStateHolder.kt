package org.mobilenativefoundation.storex.paging.internal.impl

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.mobilenativefoundation.storex.paging.internal.api.FetchingState
import org.mobilenativefoundation.storex.paging.internal.api.FetchingStateHolder

/**
 * TODO(): Make this thread safe!
 */
class RealFetchingStateHolder<Id : Comparable<Id>>(
    initialFetchingState: FetchingState<Id> = FetchingState()
) : FetchingStateHolder<Id> {
    private val _state = MutableStateFlow(initialFetchingState)
    override val state: StateFlow<FetchingState<Id>> = _state.asStateFlow()

    override fun updateMaxItemAccessedSoFar(id: Id) {
        val maxItemAccessedSoFar = _state.value.maxItemAccessedSoFar?.let { maxOf(it, id) } ?: id

        _state.value = _state.value.copy(maxItemAccessedSoFar = maxItemAccessedSoFar)
    }

    override fun update(nextState: FetchingState<Id>) {
        _state.value = nextState
    }

    override fun update(reducer: (prevState: FetchingState<Id>) -> FetchingState<Id>) {
        _state.value = reducer(_state.value)
    }

}