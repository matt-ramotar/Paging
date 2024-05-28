package org.mobilenativefoundation.storex.paging.internal.impl

import org.mobilenativefoundation.store5.core.Identifiable
import org.mobilenativefoundation.storex.paging.PagingConfig
import org.mobilenativefoundation.storex.paging.PagingSource
import org.mobilenativefoundation.storex.paging.PagingState
import org.mobilenativefoundation.storex.paging.custom.FetchingStrategy
import org.mobilenativefoundation.storex.paging.internal.api.FetchingState


/**
 * Default implementation of [FetchingStrategy].
 */
class DefaultFetchingStrategy<Id : Comparable<Id>, K : Any, V : Identifiable<Id>, E : Any>(
    pagingConfig: PagingConfig<Id, K>
) : FetchingStrategy<Id, K, E> {
    private val prefetchDistance = pagingConfig.prefetchDistance
    private var forwardPrefetchThreshold: Id? = null
    private var backwardPrefetchThreshold: Id? = null

    private fun updatePrefetchThresholds(pagingState: PagingState<Id, E>) {
        forwardPrefetchThreshold =
            pagingState.ids.getOrNull(pagingState.ids.size - prefetchDistance)
        backwardPrefetchThreshold = pagingState.ids.getOrNull(prefetchDistance - 1)
    }

    override fun shouldFetchForward(
        params: PagingSource.LoadParams<K>,
        pagingState: PagingState<Id, E>,
        fetchingState: FetchingState<Id>
    ): Boolean {
        val maxItemAccessed = fetchingState.maxItemAccessedSoFar ?: return true

        if (forwardPrefetchThreshold == null) {
            updatePrefetchThresholds(pagingState)
        }

        return forwardPrefetchThreshold?.let { maxItemAccessed >= it } ?: true
    }

    override fun shouldFetchBackward(
        params: PagingSource.LoadParams<K>,
        pagingState: PagingState<Id, E>,
        fetchingState: FetchingState<Id>
    ): Boolean {
        val minItemAccessed = fetchingState.minItemAccessedSoFar ?: return true

        if (backwardPrefetchThreshold == null) {
            updatePrefetchThresholds(pagingState)
        }

        return backwardPrefetchThreshold?.let { minItemAccessed <= it } ?: true
    }
}