package org.mobilenativefoundation.storex.paging.runtime.internal.pager.impl

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.mobilenativefoundation.storex.paging.runtime.FetchingState
import org.mobilenativefoundation.storex.paging.runtime.Identifier
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.api.FetchingStateHolder

/**
 * A thread-safe implementation of FetchingStateHolder that manages the fetching state
 * for a paging system.
 *
 * @param Id The type of the item identifier.
 * @param K The type of the paging key.
 * @param initialState The initial fetching state.
 */
class ConcurrentFetchingStateHolder<Id : Identifier<Id>, K : Comparable<K>>(
    initialState: FetchingState<Id, K>
) : FetchingStateHolder<Id, K> {

    private val _state = MutableStateFlow(initialState)
    override val state: StateFlow<FetchingState<Id, K>> = _state.asStateFlow()

    // Mutex for ensuring thread-safe access to mutable state
    private val mutex = Mutex()

    /**
     * Updates the fetching state using a reducer function.
     *
     * @param reducer A function that takes the previous state and returns a new state.
     */
    override suspend fun update(reducer: (prevState: FetchingState<Id, K>) -> FetchingState<Id, K>) {
        mutex.withLock {
            _state.value = reducer(_state.value)
        }
    }

    /**
     * Updates the fetching state to a new state.
     *
     * @param nextState The new fetching state.
     */
    override suspend fun update(nextState: FetchingState<Id, K>) {
        mutex.withLock {
            _state.value = nextState
        }
    }

    /**
     * Updates the maximum item ID accessed so far.
     *
     * @param id The ID of the item accessed.
     */
    override suspend fun updateMaxItemAccessedSoFar(id: Id) {
        update { prevState ->
            prevState.copy(maxItemAccessedSoFar = maxOf(prevState.maxItemAccessedSoFar, id))
        }
    }

    /**
     * Updates the minimum item ID accessed so far.
     *
     * @param id The ID of the item accessed.
     */
    override suspend fun updateMinItemAccessedSoFar(id: Id) {
        update { prevState ->
            prevState.copy(minItemAccessedSoFar = minOf(prevState.minItemAccessedSoFar, id))
        }
    }

    /**
     * Updates the maximum paging key requested so far.
     *
     * @param key The paging key requested.
     */
    override suspend fun updateMaxRequestSoFar(key: K) {
        update { prevState ->
            prevState.copy(maxRequestSoFar = maxOf(prevState.maxRequestSoFar, key))
        }
    }

    /**
     * Updates the minimum paging key requested so far.
     *
     * @param key The paging key requested.
     */
    override suspend fun updateMinRequestSoFar(key: K) {
        update { prevState ->
            prevState.copy(minRequestSoFar = minOf(prevState.minRequestSoFar, key))
        }
    }

    /**
     * Updates the minimum item ID loaded so far.
     *
     * @param id The ID of the item loaded.
     */
    override suspend fun updateMinItemLoadedSoFar(id: Id) {
        update { prevState ->
            prevState.copy(minItemLoadedSoFar = minOf(prevState.minItemLoadedSoFar, id))
        }
    }

    /**
     * Updates the maximum item ID loaded so far.
     *
     * @param id The ID of the item loaded.
     */
    override suspend fun updateMaxItemLoadedSoFar(id: Id) {
        update { prevState ->
            prevState.copy(maxItemLoadedSoFar = maxOf(prevState.maxItemLoadedSoFar, id))
        }
    }

    private fun maxOf(a: Id?, b: Id): Id {

        return when {
            a == null -> b
            else -> if (a > b) a else b
        }
    }

    private fun minOf(a: Id?, b: Id): Id {
        return when {
            a == null -> b
            else -> if (a < b) a else b
        }
    }

    private fun maxOf(a: K?, b: K): K {

        return when {
            a == null -> b
            else -> if (a > b) a else b
        }
    }

    private fun minOf(a: K?, b: K): K {
        return when {
            a == null -> b
            else -> if (a < b) a else b
        }
    }
}


