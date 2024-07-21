package org.mobilenativefoundation.storex.paging.runtime.internal.pager.impl

import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import org.mobilenativefoundation.storex.paging.custom.FetchingStrategy
import org.mobilenativefoundation.storex.paging.runtime.Comparator
import org.mobilenativefoundation.storex.paging.runtime.FetchingState
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
 * @param ItemId The type of the item identifier.
 * @param PageRequestKey The type of the key used for loading pages.
 */
class DefaultFetchingStrategy<ItemId : Any, PageRequestKey : Any>(
    private val pagingConfig: PagingConfig<ItemId, PageRequestKey>,
    private val logger: PagingLogger,
    private val listSortAnalyzer: ListSortAnalyzer<ItemId>,
    private val itemIdComparator: Comparator<ItemId>
) : FetchingStrategy<ItemId, PageRequestKey> {

    // Thread-safe caching of the last analyzed list and its sort order
    private data class CachedOrder<Id>(val ids: List<Id?>, val order: Order)

    private val cachedOrder = atomic<CachedOrder<ItemId>?>(null)

    /**
     * Determines if a forward fetch should be performed.
     */
    override fun shouldFetchForward(
        params: PagingSource.LoadParams<PageRequestKey>,
        pagingState: PagingState<ItemId>,
        fetchingState: FetchingState<ItemId, PageRequestKey>
    ): Boolean {
        return shouldFetch(pagingState, fetchingState, FetchDirection.FORWARD)
    }

    /**
     * Determines if a backward fetch should be performed.
     */
    override fun shouldFetchBackward(
        params: PagingSource.LoadParams<PageRequestKey>,
        pagingState: PagingState<ItemId>,
        fetchingState: FetchingState<ItemId, PageRequestKey>
    ): Boolean {
        return shouldFetch(pagingState, fetchingState, FetchDirection.BACKWARD)
    }

    /**
     * Core logic for determining whether to fetch more items.
     * This method is optimized for performance and thread safety.
     */
    private fun shouldFetch(
        pagingState: PagingState<ItemId>,
        fetchingState: FetchingState<ItemId, PageRequestKey>,
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
    private fun getOrder(ids: List<ItemId?>): Order {
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
        pagingState: PagingState<ItemId>,
        itemLoadedSoFar: ItemId?,
        itemAccessedSoFar: ItemId?,
        sortOrder: Order
    ): Boolean {

        // No item loaded yet, should fetch
        if (itemLoadedSoFar == null) return true

        // No item accessed yet, should fetch based on item count
        if (itemAccessedSoFar == null) return isUnderPrefetchLimit(pagingState, itemLoadedSoFar, sortOrder)

        val distance = getDistance(itemLoadedSoFar, itemAccessedSoFar, pagingState)

        return distance < pagingConfig.prefetchDistance
    }

    /**
     * Calculates the distance between two items.
     * Uses Comparator's distance method if available, otherwise falls back to index-based calculation.
     */
    private fun getDistance(itemLoaded: ItemId, itemAccessed: ItemId, pagingState: PagingState<ItemId>): Int {
        return itemIdComparator.distance(itemLoaded, itemAccessed) ?: calculateIndexDistance(itemLoaded, itemAccessed, pagingState)
    }

    private fun calculateIndexDistance(itemLoaded: ItemId, itemAccessed: ItemId, pagingState: PagingState<ItemId>): Int {
        var loadedIndex = -1
        var accessedIndex = -1

        for ((index, id) in pagingState.ids.withIndex()) {
            when (id) {
                itemLoaded -> loadedIndex = index
                itemAccessed -> accessedIndex = index
            }
            if (loadedIndex != -1 && accessedIndex != -1) break
        }

        return if (loadedIndex != -1 && accessedIndex != -1) {
            abs(loadedIndex - accessedIndex)
        } else {
            logger.warn("Failed to calculate distance between $itemLoaded and $itemAccessed")

            // Fetch more data if we can't determine the distance
            Int.MIN_VALUE
        }
    }

    /**
     * Checks if the loaded item is within the prefetch limit based on the sort order.
     * Inlined for performance in high-frequency calls.
     */
    private inline fun isUnderPrefetchLimit(
        pagingState: PagingState<ItemId>,
        itemLoadedSoFar: ItemId,
        sortOrder: Order
    ): Boolean {
        val ids = pagingState.ids
        val index = ids.indexOf(itemLoadedSoFar)

        // If the item is not found, we should fetch more data
        if (index == -1) return true

        val size = ids.size

        return when (sortOrder) {
            Order.ASCENDING -> index < pagingConfig.prefetchDistance
            Order.DESCENDING -> (size - 1 - index) < pagingConfig.prefetchDistance
            Order.UNKNOWN, Order.UNSORTED -> {

                val maxDistance = maxOf(
                    index, // Distance from start
                    size - 1 - index, // Distance from end
                )

                maxDistance < pagingConfig.prefetchDistance
            }
        }
    }

    /**
     * Enum representing the direction of fetching.
     */
    private enum class FetchDirection {
        FORWARD,
        BACKWARD
    }
}