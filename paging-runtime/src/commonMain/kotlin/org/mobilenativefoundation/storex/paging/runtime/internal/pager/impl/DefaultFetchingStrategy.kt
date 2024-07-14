package org.mobilenativefoundation.storex.paging.runtime.internal.pager.impl

import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import org.mobilenativefoundation.storex.paging.custom.FetchingStrategy
import org.mobilenativefoundation.storex.paging.runtime.FetchingState
import org.mobilenativefoundation.storex.paging.runtime.Identifier
import org.mobilenativefoundation.storex.paging.runtime.PagingConfig
import org.mobilenativefoundation.storex.paging.runtime.PagingSource
import org.mobilenativefoundation.storex.paging.runtime.PagingState
import org.mobilenativefoundation.storex.paging.runtime.internal.logger.api.PagingLogger
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.api.ListSortAnalyzer
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.api.ListSortAnalyzer.Order
import kotlin.math.abs

/**
 * Thread-safe implementation of [FetchingStrategy].
 * 1. Uses [ListSortAnalyzer] to determine [Order].
 * 2. Decides whether to fetch more items based on the current [PagingState] and [FetchingState].
 *
 * @param Id The type of the identifier for items in the dataset, must implement [Identifier].
 * @param K The type of the key used for loading pages.
 */
class DefaultFetchingStrategy<Id : Identifier<Id>, K : Any>(
    private val pagingConfig: PagingConfig<Id, K>,
    private val logger: PagingLogger,
    private val listSortAnalyzer: ListSortAnalyzer<Id>
) : FetchingStrategy<Id, K> {

    // Thread-safe caching of the last analyzed list and its sort order
    private data class CachedOrder<Id>(val ids: List<Id?>, val order: Order)

    private val cachedOrder = atomic<CachedOrder<Id>?>(null)

    /**
     * Determines if a forward fetch should be performed.
     */
    override fun shouldFetchForward(
        params: PagingSource.LoadParams<K>,
        pagingState: PagingState<Id>,
        fetchingState: FetchingState<Id, K>
    ): Boolean {
        return shouldFetch(pagingState, fetchingState, FetchDirection.FORWARD)
    }

    /**
     * Determines if a backward fetch should be performed.
     */
    override fun shouldFetchBackward(
        params: PagingSource.LoadParams<K>,
        pagingState: PagingState<Id>,
        fetchingState: FetchingState<Id, K>
    ): Boolean {
        return shouldFetch(pagingState, fetchingState, FetchDirection.BACKWARD)
    }

    /**
     * Core logic for determining whether to fetch more items.
     * This method is optimized for performance and thread safety.
     */
    private fun shouldFetch(
        pagingState: PagingState<Id>,
        fetchingState: FetchingState<Id, K>,
        fetchDirection: FetchDirection
    ): Boolean {

        logger.debug(
            """
            Deciding whether to fetch
                Paging state: $pagingState
                Fetching state: $fetchingState
                Fetch direction: $fetchDirection
            """.trimIndent()
        )

        // Get the current sort order, using cached value if available
        val order = getOrder(pagingState.ids)

        // Determine which items to consider based on sort order and fetch direction
        val (itemLoadedSoFar, itemAccessedSoFar) = when (order) {
            Order.ASCENDING -> when (fetchDirection) {
                FetchDirection.FORWARD -> fetchingState.maxItemLoadedSoFar to fetchingState.maxItemAccessedSoFar
                FetchDirection.BACKWARD -> fetchingState.minItemLoadedSoFar to fetchingState.minItemAccessedSoFar
            }

            Order.DESCENDING -> when (fetchDirection) {
                FetchDirection.FORWARD -> fetchingState.minItemLoadedSoFar to fetchingState.minItemAccessedSoFar
                FetchDirection.BACKWARD -> fetchingState.maxItemLoadedSoFar to fetchingState.maxItemAccessedSoFar
            }

            else -> {
                // For unknown or unsorted lists, check both ascending and descending scenarios
                val ascendingPair = when (fetchDirection) {
                    FetchDirection.FORWARD -> fetchingState.maxItemLoadedSoFar to fetchingState.maxItemAccessedSoFar
                    FetchDirection.BACKWARD -> fetchingState.minItemLoadedSoFar to fetchingState.minItemAccessedSoFar
                }
                val descendingPair = when (fetchDirection) {
                    FetchDirection.FORWARD -> fetchingState.minItemLoadedSoFar to fetchingState.minItemAccessedSoFar
                    FetchDirection.BACKWARD -> fetchingState.maxItemLoadedSoFar to fetchingState.maxItemAccessedSoFar
                }
                return checkFetchCondition(pagingState, ascendingPair.first, ascendingPair.second, Order.ASCENDING) ||
                    checkFetchCondition(pagingState, descendingPair.first, descendingPair.second, Order.DESCENDING)
            }
        }

        val shouldFetch = checkFetchCondition(pagingState, itemLoadedSoFar, itemAccessedSoFar, order)
        logger.debug("Should fetch: $shouldFetch")
        return shouldFetch
    }

    /**
     * Retrieves the sort order of the list, using a cached value if available.
     * This method ensures thread-safe access and updates to the cached order.
     */
    private fun getOrder(ids: List<Id?>): Order {
        val cached = cachedOrder.value
        return if (cached?.ids == ids) {
            cached.order
        } else {
            val newOrder = listSortAnalyzer(ids)
            cachedOrder.update { CachedOrder(ids, newOrder) }
            newOrder
        }
    }

    /**
     * Checks if fetching should occur based on the sort order and distance between loaded and accessed items.
     * Inlined for performance in high-frequency calls.
     */
    private inline fun checkFetchCondition(
        pagingState: PagingState<Id>,
        itemLoadedSoFar: Id?,
        itemAccessedSoFar: Id?,
        sortOrder: Order
    ): Boolean {
        return itemLoadedSoFar?.let { itemLoaded ->
            itemAccessedSoFar?.let { itemAccessed ->
                val distance = distanceBetween(itemLoaded, itemAccessed)
                when (sortOrder) {
                    Order.ASCENDING -> itemAccessed > itemLoaded && distance < pagingConfig.prefetchDistance
                    Order.DESCENDING -> itemAccessed < itemLoaded && distance < pagingConfig.prefetchDistance
                    Order.UNKNOWN, Order.UNSORTED -> distance < pagingConfig.prefetchDistance
                }
            } ?: isUnderPrefetchLimit(pagingState, itemLoadedSoFar, sortOrder)
        } ?: true
    }

    /**
     * Checks if the loaded item is within the prefetch limit based on the sort order.
     * Inlined for performance in high-frequency calls.
     */
    private inline fun isUnderPrefetchLimit(
        pagingState: PagingState<Id>,
        itemLoadedSoFar: Id,
        sortOrder: Order
    ): Boolean {
        val index = pagingState.ids.indexOfFirst { it == itemLoadedSoFar }
        if (index == -1) return true
        return when (sortOrder) {
            Order.ASCENDING -> index < pagingConfig.prefetchDistance - 1
            Order.DESCENDING -> pagingState.ids.size - index - 1 < pagingConfig.prefetchDistance - 1
            Order.UNKNOWN, Order.UNSORTED ->
                index < pagingConfig.prefetchDistance - 1 ||
                    pagingState.ids.size - index - 1 < pagingConfig.prefetchDistance - 1
        }
    }

    /**
     * Calculates the absolute distance between two items.
     */
    private fun distanceBetween(itemLoaded: Id, itemAccessed: Id): Int {
        return abs(itemAccessed - itemLoaded)
    }

    /**
     * Enum representing the direction of fetching.
     */
    private enum class FetchDirection {
        FORWARD,
        BACKWARD
    }
}