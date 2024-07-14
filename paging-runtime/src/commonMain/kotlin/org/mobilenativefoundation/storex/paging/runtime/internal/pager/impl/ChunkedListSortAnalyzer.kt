package org.mobilenativefoundation.storex.paging.runtime.internal.pager.impl

import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import org.mobilenativefoundation.storex.paging.runtime.Identifier
import org.mobilenativefoundation.storex.paging.runtime.internal.logger.api.PagingLogger
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.api.ListSortAnalyzer
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.api.ListSortAnalyzer.Order
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.impl.ChunkedListSortAnalyzer.CacheEntry
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.impl.ChunkedListSortAnalyzer.ChunkInfo


/**
 * Thread-safe implementation of [ListSortAnalyzer] optimized for large lists of [Id]s.
 *
 * This class uses a chunk-based caching strategy to efficiently analyze and cache [Order]:
 * 1. It divides the input list into smaller chunks based on [chunkSize].
 * 2. Then it processes each chunk individually.
 * 3. Finally, it combines the results to determine the overall [Order].
 *
 * **Recommendation**: Use [DefaultListSortAnalyzer] if list size will be less than 1000 elements.
 * While this implementation is efficient for large lists, it will be suboptimal for small to medium-sized lists:
 * 1. **Chunking overhead**: Dividing the list into chunks and processing each chunk separately is not necessary for smaller lists.
 * 2. **Caching complexity**: Unnecessarily complex for smaller lists.
 * 3. **Extra function calls**: Costs of calls to [invoke], [determineOrder], [determineOverallOrder] will be more noticeable with smaller lists.
 * 4. **Memory usage**: Creating [ChunkInfo] and [CacheEntry] objects is unnecessary for smaller lists and wastes memory.
 *
 * @param Id The type of the identifier for items in the dataset, must implement [Identifier].
 * @property logger A [PagingLogger] instance for debug logging.
 * @property chunkSize The size of each chunk when processing the list. Default is 100.
 *
 * @see [DefaultListSortAnalyzer]
 */
class ChunkedListSortAnalyzer<Id : Identifier<Id>>(
    private val logger: PagingLogger,
    private val chunkSize: Int = 100 // TODO(): Adaptive chunk sizes based on input list size could optimize performance for varying input sizes.
) : ListSortAnalyzer<Id> {

    /**
     * Represents information about a single chunk of the input list.
     *
     * @property hash The hash code of the chunk, used for quick equality checks.
     * @property sortOrder The determined sort order of the chunk.
     */
    private data class ChunkInfo(val hash: Int, val sortOrder: Order)

    /**
     * Represents a cached entry, containing information about all chunks and the overall sort order.
     *
     * @property chunks A list of [ChunkInfo] objects, each representing a chunk of the input list.
     * @property overallOrder The determined sort order for the entire input list.
     */
    private data class CacheEntry<Id>(val chunks: List<ChunkInfo>, val overallOrder: Order)


    // Thread-safe atomic reference to the cached entry
    private val cache = atomic<CacheEntry<Id>?>(null)

    /**
     * Determines the sort order of the given list of identifiers.
     *
     * This method divides the input list into chunks, processes each chunk, and combines
     * the results to determine the overall sort order. It uses caching to improve performance
     * for repeated calls with similar data.
     *
     * @param ids The list of identifiers to analyze.
     * @return The determined [ListSortAnalyzer.Order] of the input list.
     */
    override operator fun invoke(ids: List<Id?>): Order {
        // Quick return if it's not yet possible to determine sort order
        if (ids.size < 2) return Order.UNKNOWN

        // Divide the input list into chunks
        val chunks = ids.chunked(chunkSize)

        // Process each chunk and create ChunkInfo objects
        val chunkInfos = chunks.map { chunk ->
            // TODO(): Figure out how to detect changes more efficiently, this will be slow for large chunks.
            val hash = chunk.hashCode()

            ChunkInfo(hash, determineOrder(chunk))
        }

        // Check if we can use the cached result
        cache.value?.let { entry ->
            if (entry.chunks == chunkInfos) {
                logger.debug("Using fully cached sort order: ${entry.overallOrder}")
                return entry.overallOrder
            }
        }

        // Determine the overall sort order based on chunk information
        // TODO(): Currently processing chunks sequentially. For very large lists, processing chunks in parallel could significantly improve performance.
        val overallOrder = determineOverallOrder(chunkInfos)

        // Update the cache with the new chunk information
        cache.update { CacheEntry(chunkInfos, overallOrder) }

        logger.debug("Overall sort order determined: $overallOrder")
        return overallOrder
    }

    /**
     * Determines the sort order of a single chunk of identifiers.
     *
     * This method checks if the chunk is strictly ascending, strictly descending,
     * or unsorted by comparing adjacent elements.
     *
     * @param chunk A list of identifiers representing a chunk of the original input.
     * @return The [Order] of the chunk.
     */
    private fun determineOrder(chunk: List<Id?>): Order {
        var ascending = true
        var descending = true

        for (i in 0 until chunk.size - 1) {
            val current = chunk[i]
            val next = chunk[i + 1]

            if (current == null || next == null) {
                // Continue if not possible to compare
                continue
            }

            // Only check if ascending not already ruled out
            if (ascending) {
                // Check if the order violates ascending
                if (current >= next) ascending = false
            }

            // Only check if descending not already ruled out
            if (descending) {
                // Check if the order violates descending
                if (current <= next) descending = false

            }

            // Break if now unsorted
            if (!ascending && !descending) break
        }

        return when {
            ascending -> Order.ASCENDING
            descending -> Order.DESCENDING
            else -> Order.UNSORTED
        }
    }

    /**
     * Determines the overall sort order based on the sort orders of individual chunks.
     *
     * This method checks if all chunks have the same sort order (either all ascending
     * or all descending). If not, it considers the overall list as unsorted.
     *
     * @param chunkInfos A list of [ChunkInfo] objects representing all chunks.
     * @return The overall [Order] of the entire list.
     */
    private fun determineOverallOrder(chunkInfos: List<ChunkInfo>): Order {
        return when {
            chunkInfos.all { it.sortOrder == Order.ASCENDING } -> Order.ASCENDING
            chunkInfos.all { it.sortOrder == Order.DESCENDING } -> Order.DESCENDING
            else -> Order.UNSORTED
        }
    }

}