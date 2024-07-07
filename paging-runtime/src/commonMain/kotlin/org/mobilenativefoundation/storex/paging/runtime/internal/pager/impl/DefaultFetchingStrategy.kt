package org.mobilenativefoundation.storex.paging.runtime.internal.pager.impl

import org.mobilenativefoundation.storex.paging.custom.FetchingStrategy
import org.mobilenativefoundation.storex.paging.runtime.FetchingState
import org.mobilenativefoundation.storex.paging.runtime.Identifier
import org.mobilenativefoundation.storex.paging.runtime.PagingConfig
import org.mobilenativefoundation.storex.paging.runtime.internal.logger.api.PagingLogger
import org.mobilenativefoundation.storex.paging.runtime.PagingSource
import org.mobilenativefoundation.storex.paging.runtime.PagingState
import kotlin.math.abs

class DefaultFetchingStrategy<Id : Identifier<Id>, K : Any>(
    private val pagingConfig: PagingConfig<Id, K>,
    private val logger: PagingLogger
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
        logger.debug("Deciding whether to fetch")
        logger.debug("Current paging state: $pagingState")
        logger.debug("Current fetching state: $fetchingState")
        logger.debug("Fetch direction: $fetchDirection")

        val minItemLoadedSoFar = fetchingState.minItemLoadedSoFar
        val maxItemLoadedSoFar = fetchingState.maxItemLoadedSoFar
        val minItemAccessedSoFar = fetchingState.minItemAccessedSoFar
        val maxItemAccessedSoFar = fetchingState.maxItemAccessedSoFar

        val fetch = when (pagingState.determineSortOrder()) {
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

        logger.debug("Should fetch: $fetch")

        return fetch
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

    private fun distanceBetween(
        itemLoaded: Id,
        itemAccessed: Id,
    ): Int {
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
                val distance = distanceBetween(itemLoaded, itemAccessed)
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