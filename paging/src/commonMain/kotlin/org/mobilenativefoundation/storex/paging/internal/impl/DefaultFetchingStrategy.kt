package org.mobilenativefoundation.storex.paging.internal.impl

import org.mobilenativefoundation.storex.paging.*
import org.mobilenativefoundation.storex.paging.custom.FetchingStrategy
import org.mobilenativefoundation.storex.paging.internal.api.FetchingState


/**
 * Default implementation of [FetchingStrategy].
 */
class DefaultFetchingStrategy<Id : Comparable<Id>, Q : Quantifiable<Id>, K : Any, V : Identifiable<Id, Q>, E : Any>(
    private val pagingConfig: PagingConfig<Id, Q, K>
) : FetchingStrategy<Id, Q, K, E> {
    private val prefetchDistance = pagingConfig.prefetchDistance


    private fun minItemLoadedIsUnderPrefetchLimit(pagingState: PagingState<Id, Q, E>, minLoaded: Q): Boolean {
        val indexOfMinLoaded = pagingState.ids.indexOfFirst { it?.value == minLoaded.value }
        return indexOfMinLoaded < pagingConfig.prefetchDistance
    }

    override fun shouldFetchForward(
        params: PagingSource.LoadParams<K>,
        pagingState: PagingState<Id, Q, E>,
        fetchingState: FetchingState<Id, Q, K>
    ): Boolean {
        println("SHOULD FETCH FOWRARD HITTING")

        val isUnderPrefetchLimit: Boolean =
            (fetchingState.minItemLoadedSoFar?.let { minLoaded ->
                fetchingState.minItemAccessedSoFar?.let { minAccessed ->

                    // TODO(): This is assuming that forward fetching is getting decreasing ids
                    minAccessed - minLoaded < prefetchDistance
                } ?: minItemLoadedIsUnderPrefetchLimit(pagingState, minLoaded)
            }) ?: true // No item loaded or accessed, so should load

        return isUnderPrefetchLimit
    }

    override fun shouldFetchBackward(
        params: PagingSource.LoadParams<K>,
        pagingState: PagingState<Id, Q, E>,
        fetchingState: FetchingState<Id, Q, K>
    ): Boolean {
        TODO()
    }
}