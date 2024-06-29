package org.mobilenativefoundation.storex.paging.internal.impl

import org.mobilenativefoundation.storex.paging.*
import org.mobilenativefoundation.storex.paging.custom.FetchingStrategy
import org.mobilenativefoundation.storex.paging.internal.api.FetchingState
import kotlin.math.abs

class DefaultFetchingStrategy<Id : Identifier<Id>, K : Any, V : Identifiable<Id>>(
    private val pagingConfig: PagingConfig<Id, K>
) : FetchingStrategy<Id, K> {

    override fun shouldFetchForward(
        params: PagingSource.LoadParams<K>,
        pagingState: PagingState<Id>,
        fetchingState: FetchingState<Id, K>
    ): Boolean {
        return shouldFetch(pagingState, fetchingState, FetchDirection.FORWARD)
    }

    override fun shouldFetchBackward(
        params: PagingSource.LoadParams<K>,
        pagingState: PagingState<Id>,
        fetchingState: FetchingState<Id, K>
    ): Boolean {
        return shouldFetch(pagingState, fetchingState, FetchDirection.BACKWARD)
    }

    private fun shouldFetch(
        pagingState: PagingState<Id>,
        fetchingState: FetchingState<Id, K>,
        fetchDirection: FetchDirection
    ): Boolean {
        println("DECIDING WHETHER TO FETCH")
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

    private fun PagingState<Id>.determineSortOrder(): SortOrder {
        val firstItem = ids.firstOrNull()
        val lastItem = ids.lastOrNull()

        return if (firstItem != null && lastItem != null) {
            if (firstItem < lastItem) SortOrder.ASCENDING else SortOrder.DESCENDING
        } else {
            SortOrder.UNKNOWN
        }
    }

    private fun isUnderPrefetchLimit(pagingState: PagingState<Id>, itemLoadedSoFar: Id?): Boolean {
        if (itemLoadedSoFar == null) return true
        val index = pagingState.ids.indexOfFirst { it == itemLoadedSoFar }
        if (index == -1) return true
        return index < pagingConfig.prefetchDistance - 1
    }

    private fun PagingState<Id>.distanceBetween(itemLoaded: Id, itemAccessed: Id, sortOrder: SortOrder): Int {
        return abs(itemAccessed - itemLoaded)
    }

    private fun checkFetchCondition(
        pagingState: PagingState<Id>,
        itemLoadedSoFar: Id?,
        itemAccessedSoFar: Id?,
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