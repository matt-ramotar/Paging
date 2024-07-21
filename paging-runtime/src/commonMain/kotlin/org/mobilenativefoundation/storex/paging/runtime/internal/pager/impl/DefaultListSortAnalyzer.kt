package org.mobilenativefoundation.storex.paging.runtime.internal.pager.impl

import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import org.mobilenativefoundation.storex.paging.runtime.Comparator
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.api.ListSortAnalyzer
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.api.ListSortAnalyzer.Order

/**
 * Thread-safe implementation of [ListSortAnalyzer] optimized for small to medium-sized lists of [ItemId]s.
 *
 * This class uses a single-pass analysis strategy with a simple caching mechanism:
 * 1. It analyzes the entire list in a single pass, checking for ascending and descending order simultaneously.
 * 2. It terminates early when the list is determined to be unsorted.
 * 3. It caches recent results to improve performance for repeated calls with identical data.
 *
 * **Recommendation**: Use this analyzer if list size will be less than 1000 elements.
 * This implementation is efficient for small to medium-sized lists due to:
 * 1. **Minimal overhead**: No chunking or complex processing steps.
 * 2. **Simple caching**: Uses a straightforward caching mechanism suitable for smaller datasets.
 * 3. **Reduced function calls**: All logic is contained within a single function, minimizing call overhead.
 * 4. **Efficient memory usage**: Avoids creating unnecessary objects during analysis.
 *
 * @param ItemId The type of the item identifier.
 * @property maxCacheSize The maximum number of recent results to cache. Default is 5.
 *
 * @see [ChunkedListSortAnalyzer]
 */
class DefaultListSortAnalyzer<ItemId : Any>(
    private val itemIdComparator: Comparator<ItemId>,
    private val maxCacheSize: Int = 5,
) : ListSortAnalyzer<ItemId> {

    /**
     * Represents a cached entry, containing the analyzed list and its determined order.
     */
    private data class CacheEntry<Id>(val list: List<Id?>, val order: Order)

    // Thread-safe atomic reference to the cached entries
    private val cache = atomic(listOf<CacheEntry<ItemId>>())

    /**
     * Determines the sort order of the given list of identifiers.
     *
     * This method analyzes the input list in a single pass to determine if it's
     * sorted in ascending order, descending order, or unsorted. It uses a simple
     * caching mechanism to improve performance for repeated calls with identical data.
     *
     * @param ids The list of identifiers to analyze.
     * @return The determined [Order] of the input list.
     */
    override operator fun invoke(ids: List<ItemId?>): Order {
        // Quick return for lists with fewer than 2 elements
        if (ids.size < 2) return Order.UNKNOWN

        // Check cache for a matching entry
        cache.value.find { it.list == ids }?.let { return it.order }

        var ascending = true
        var descending = true

        // Single-pass analysis of the list
        for (i in 0 until ids.size - 1) {
            val current = ids[i]
            val next = ids[i + 1]
            if (current != null && next != null) {
                when (itemIdComparator.compare(current, next)) {
                    1 -> {
                        ascending = false
                        // Early termination if we've already ruled out descending order
                        if (!descending) break
                    }

                    -1 -> {
                        descending = false
                        // Early termination if we've already ruled out ascending order
                        if (!ascending) break
                    }

                    0 -> {
                        // If current == next, we continue checking the next pair
                    }
                }
            }
        }

        // Determine the final order based on the analysis
        val result = when {
            ascending -> Order.ASCENDING
            descending -> Order.DESCENDING
            else -> Order.UNSORTED
        }

        // Update cache with the new result
        updateCache(ids, result)

        return result
    }

    /**
     * Updates the cache with a new analysis result.
     *
     * This method adds the new result to the cache and ensures the cache doesn't
     * exceed the maximum size by removing the oldest entries if necessary.
     *
     * @param ids The analyzed list of identifiers.
     * @param order The determined order of the list.
     */
    private fun updateCache(ids: List<ItemId?>, order: Order) {
        cache.update { currentCache ->
            (currentCache + CacheEntry(ids, order))
                .takeLast(maxCacheSize)
        }
    }
}