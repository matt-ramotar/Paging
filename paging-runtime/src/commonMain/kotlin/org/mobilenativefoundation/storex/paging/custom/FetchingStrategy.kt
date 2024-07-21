package org.mobilenativefoundation.storex.paging.custom

import org.mobilenativefoundation.storex.paging.runtime.FetchingState
import org.mobilenativefoundation.storex.paging.runtime.PagingSource
import org.mobilenativefoundation.storex.paging.runtime.PagingState


/**
 * Represents a strategy for determining whether to fetch more data based on the current state of the pager.
 * The fetching strategy is responsible for deciding whether to fetch more data based on the anchor position,
 * prefetch position, paging configuration, and the current state of the paging buffer.
 *
 * Implementing a custom [FetchingStrategy] allows you to define your own logic for when to fetch more data.
 * For example, you can fetch more data when the user scrolls near the end of the currently loaded data,
 * or when a certain number of items are remaining in the buffer.
 *
 * @param ItemId The type of the item identifier.
 * @param PageRequestKey The type of the paging key.
 */
interface FetchingStrategy<ItemId : Any, PageRequestKey : Any> {

    /**
     * Determines whether to fetch more data in the forward direction based on the current state of the pager.
     *
     * @param params The load parameters for the potential fetch operation.
     * @param pagingState The current state of the pager.
     * @param fetchingState The current fetching state, including information about accessed items.
     * @return true if more data should be fetched, false otherwise.
     */
    fun shouldFetchForward(
        params: PagingSource.LoadParams<PageRequestKey>,
        pagingState: PagingState<ItemId>,
        fetchingState: FetchingState<ItemId, PageRequestKey>,
    ): Boolean

    /**
     * Determines whether to fetch more data in the backward direction based on the current state of the pager.
     *
     * @param params The load parameters for the potential fetch operation.
     * @param pagingState The current state of the pager.
     * @param fetchingState The current fetching state, including information about accessed items.
     * @return true if more data should be fetched, false otherwise.
     */
    fun shouldFetchBackward(
        params: PagingSource.LoadParams<PageRequestKey>,
        pagingState: PagingState<ItemId>,
        fetchingState: FetchingState<ItemId, PageRequestKey>,
    ): Boolean
}