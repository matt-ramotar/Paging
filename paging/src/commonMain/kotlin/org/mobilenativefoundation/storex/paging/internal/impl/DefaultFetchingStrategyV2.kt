package org.mobilenativefoundation.storex.paging.internal.impl

import org.mobilenativefoundation.storex.paging.*
import org.mobilenativefoundation.storex.paging.custom.FetchingStrategy
import org.mobilenativefoundation.storex.paging.internal.api.FetchingState

class DefaultFetchingStrategyV2<Id : Comparable<Id>, Q : Quantifiable<Id>, K : Any, V : Identifiable<Id, Q>, E : Any>(
    private val pagingConfig: PagingConfig<Id, Q, K>
) : FetchingStrategy<Id, Q, K, E> {

    override fun shouldFetchForward(
        params: PagingSource.LoadParams<K>,
        pagingState: PagingState<Id, Q, E>,
        fetchingState: FetchingState<Id, Q, K>
    ): Boolean {
        return shouldFetch(pagingState, fetchingState, FetchDirection.FORWARD)
    }

    override fun shouldFetchBackward(
        params: PagingSource.LoadParams<K>,
        pagingState: PagingState<Id, Q, E>,
        fetchingState: FetchingState<Id, Q, K>
    ): Boolean {
        return shouldFetch(pagingState, fetchingState, FetchDirection.BACKWARD)
    }

    private fun shouldFetch(
        pagingState: PagingState<Id, Q, E>,
        fetchingState: FetchingState<Id, Q, K>,
        fetchDirection: FetchDirection
    ): Boolean {
        val minItemLoadedSoFar = fetchingState.minItemLoadedSoFar
        val maxItemLoadedSoFar = fetchingState.maxItemLoadedSoFar
        val minItemAccessedSoFar = fetchingState.minItemAccessedSoFar
        val maxItemAccessedSoFar = fetchingState.maxItemAccessedSoFar

        return when (pagingState.determineSortOrder()) {
            SortOrder.ASCENDING -> {
                when (fetchDirection) {
                    FetchDirection.FORWARD -> checkFetchCondition(
                        pagingState, maxItemLoadedSoFar, maxItemAccessedSoFar, SortOrder.ASCENDING
                    )

                    FetchDirection.BACKWARD -> checkFetchCondition(
                        pagingState, minItemLoadedSoFar, minItemAccessedSoFar, SortOrder.ASCENDING
                    )
                }
            }

            SortOrder.DESCENDING -> {
                when (fetchDirection) {
                    FetchDirection.FORWARD -> checkFetchCondition(
                        pagingState, minItemLoadedSoFar, minItemAccessedSoFar, SortOrder.DESCENDING
                    )

                    FetchDirection.BACKWARD -> checkFetchCondition(
                        pagingState, maxItemLoadedSoFar, maxItemAccessedSoFar, SortOrder.DESCENDING
                    )
                }
            }

            SortOrder.UNKNOWN -> {
                val shouldFetchAscending = when (fetchDirection) {
                    FetchDirection.FORWARD -> checkFetchCondition(
                        pagingState, maxItemLoadedSoFar, maxItemAccessedSoFar, SortOrder.ASCENDING
                    )

                    FetchDirection.BACKWARD -> checkFetchCondition(
                        pagingState, minItemLoadedSoFar, minItemAccessedSoFar, SortOrder.ASCENDING
                    )
                }

                val shouldFetchDescending = when (fetchDirection) {
                    FetchDirection.FORWARD -> checkFetchCondition(
                        pagingState, minItemLoadedSoFar, minItemAccessedSoFar, SortOrder.DESCENDING
                    )

                    FetchDirection.BACKWARD -> checkFetchCondition(
                        pagingState, maxItemLoadedSoFar, maxItemAccessedSoFar, SortOrder.DESCENDING
                    )
                }

                shouldFetchAscending || shouldFetchDescending
            }
        }
    }

    private fun PagingState<Id, Q, E>.determineSortOrder(): SortOrder {
        val firstItem = ids.firstOrNull()
        val lastItem = ids.lastOrNull()

        return if (firstItem != null && lastItem != null) {
            if (firstItem.value < lastItem.value) SortOrder.ASCENDING else SortOrder.DESCENDING
        } else {
            SortOrder.UNKNOWN
        }
    }

    private fun isUnderPrefetchLimit(pagingState: PagingState<Id, Q, E>, itemLoadedSoFar: Q?): Boolean {
        TODO()
    }

    private fun PagingState<Id, Q, E>.distanceBetween(itemLoaded: Q?, itemAccessed: Q?, sortOrder: SortOrder): Int {
        TODO()
    }

    private fun checkFetchCondition(
        pagingState: PagingState<Id, Q, E>,
        itemLoadedSoFar: Q?,
        itemAccessedSoFar: Q?,
        sortOrder: SortOrder
    ): Boolean {
        return itemLoadedSoFar?.let { itemLoaded ->
            itemAccessedSoFar?.let { itemAccessed ->
                val distance = pagingState.distanceBetween(itemLoaded, itemAccessed, sortOrder)
                distance < pagingConfig.prefetchDistance
            } ?: isUnderPrefetchLimit(pagingState, itemLoadedSoFar)
        } ?: true
    }

    private enum class FetchDirection {
        FORWARD,
        BACKWARD
    }

    private enum class SortOrder {
        ASCENDING,
        DESCENDING,
        UNKNOWN
    }
}