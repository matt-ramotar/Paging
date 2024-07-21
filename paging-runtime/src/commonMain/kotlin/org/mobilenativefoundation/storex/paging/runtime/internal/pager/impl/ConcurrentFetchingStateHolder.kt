package org.mobilenativefoundation.storex.paging.runtime.internal.pager.impl

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.mobilenativefoundation.storex.paging.runtime.Comparator
import org.mobilenativefoundation.storex.paging.runtime.FetchingState
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.api.FetchingStateHolder

/**
 * A thread-safe implementation of FetchingStateHolder that manages the fetching state
 * for a paging system.
 *
 * @param ItemId The type of the item identifier.
 * @param PageRequestKey The type of the paging key.
 * @param initialState The initial fetching state.
 */
class ConcurrentFetchingStateHolder<ItemId : Any, PageRequestKey : Any>(
    initialState: FetchingState<ItemId, PageRequestKey>,
    private val itemIdComparator: Comparator<ItemId>,
    private val pageRequestKeyComparator: Comparator<PageRequestKey>
) : FetchingStateHolder<ItemId, PageRequestKey> {

    private val _state = MutableStateFlow(initialState)
    override val state: StateFlow<FetchingState<ItemId, PageRequestKey>> = _state.asStateFlow()

    // Mutex for ensuring thread-safe access to mutable state
    private val mutex = Mutex()

    /**
     * Updates the fetching state using a reducer function.
     *
     * @param reducer A function that takes the previous state and returns a new state.
     */
    override suspend fun update(reducer: (prevState: FetchingState<ItemId, PageRequestKey>) -> FetchingState<ItemId, PageRequestKey>) {
        mutex.withLock {
            _state.value = reducer(_state.value)
        }
    }

    /**
     * Updates the fetching state to a new state.
     *
     * @param nextState The new fetching state.
     */
    override suspend fun update(nextState: FetchingState<ItemId, PageRequestKey>) {
        mutex.withLock {
            _state.value = nextState
        }
    }

    /**
     * Updates the maximum item ID accessed so far.
     *
     * @param id The ID of the item accessed.
     */
    override suspend fun updateMaxItemAccessedSoFar(id: ItemId) {
        update { prevState ->
            prevState.copy(maxItemAccessedSoFar = maxOfItemId(prevState.maxItemAccessedSoFar, id))
        }
    }

    /**
     * Updates the minimum item ID accessed so far.
     *
     * @param id The ID of the item accessed.
     */
    override suspend fun updateMinItemAccessedSoFar(id: ItemId) {
        update { prevState ->
            prevState.copy(minItemAccessedSoFar = minOfItemId(prevState.minItemAccessedSoFar, id))
        }
    }

    /**
     * Updates the maximum paging key requested so far.
     *
     * @param key The paging key requested.
     */
    override suspend fun updateMaxRequestSoFar(key: PageRequestKey) {
        update { prevState ->
            prevState.copy(maxRequestSoFar = maxOfPageRequestKey(prevState.maxRequestSoFar, key))
        }
    }

    /**
     * Updates the minimum paging key requested so far.
     *
     * @param key The paging key requested.
     */
    override suspend fun updateMinRequestSoFar(key: PageRequestKey) {
        update { prevState ->
            prevState.copy(minRequestSoFar = minOfPageRequestKey(prevState.minRequestSoFar, key))
        }
    }

    /**
     * Updates the minimum item ID loaded so far.
     *
     * @param id The ID of the item loaded.
     */
    override suspend fun updateMinItemLoadedSoFar(id: ItemId) {
        update { prevState ->
            prevState.copy(minItemLoadedSoFar = minOfItemId(prevState.minItemLoadedSoFar, id))
        }
    }

    /**
     * Updates the maximum item ID loaded so far.
     *
     * @param id The ID of the item loaded.
     */
    override suspend fun updateMaxItemLoadedSoFar(id: ItemId) {
        update { prevState ->
            prevState.copy(maxItemLoadedSoFar = maxOfItemId(prevState.maxItemLoadedSoFar, id))
        }
    }

    private fun maxOfItemId(a: ItemId?, b: ItemId): ItemId {

        return when {
            a == null -> b
            else -> if (itemIdComparator.compare(a, b) > 0) a else b
        }
    }

    private fun minOfItemId(a: ItemId?, b: ItemId): ItemId {
        return when {
            a == null -> b
            else -> if (itemIdComparator.compare(a, b) < 0) a else b
        }
    }

    private fun maxOfPageRequestKey(a: PageRequestKey?, b: PageRequestKey): PageRequestKey {

        return when {
            a == null -> b
            else -> if (pageRequestKeyComparator.compare(a, b) > 0) a else b
        }
    }

    private fun minOfPageRequestKey(a: PageRequestKey?, b: PageRequestKey): PageRequestKey {
        return when {
            a == null -> b
            else -> if (pageRequestKeyComparator.compare(a, b) < 0) a else b
        }
    }
}


