package org.mobilenativefoundation.storex.paging.internal.impl

import org.mobilenativefoundation.storex.paging.*
import org.mobilenativefoundation.storex.paging.custom.FetchingStrategy
import org.mobilenativefoundation.storex.paging.internal.api.FetchingState


/**
 * Default implementation of [FetchingStrategy].
 */
class DefaultFetchingStrategy<Id : Comparable<Id>, Q : Quantifiable<Id>, K : Any, V : Identifiable<Id, Q>, E : Any>(
    pagingConfig: PagingConfig<Id, Q, K>
) : FetchingStrategy<Id,Q, K, E> {
    private val prefetchDistance = pagingConfig.prefetchDistance


    override fun shouldFetchForward(
        params: PagingSource.LoadParams<K>,
        pagingState: PagingState<Id, Q, E>,
        fetchingState: FetchingState<Id, K>
    ): Boolean {
        println("SHOULD FETCH FOWRARD HITTING")


        val isUnderPrefetchLimit: Boolean =
            (fetchingState.maxItemAccessedSoFar?.let { maxItemAccessedSoFar ->
                fetchingState.currentForwardPrefetchOffset?.let { currentForwardPrefetchOffset ->
                    currentForwardPrefetchOffset.minus(maxItemAccessedSoFar) > prefetchDistance
                } ?: false
            } ?: (pagingState.ids.size < prefetchDistance))


        return isUnderPrefetchLimit
    }

    override fun shouldFetchBackward(
        params: PagingSource.LoadParams<K>,
        pagingState: PagingState<Id, Q, E>,
        fetchingState: FetchingState<Id, K>
    ): Boolean {
        TODO()
    }
}