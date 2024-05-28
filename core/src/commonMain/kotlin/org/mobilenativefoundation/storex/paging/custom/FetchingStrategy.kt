package org.mobilenativefoundation.storex.paging.custom

import org.mobilenativefoundation.storex.paging.PagingSource
import org.mobilenativefoundation.storex.paging.PagingState
import org.mobilenativefoundation.storex.paging.internal.api.FetchingState

/**
 * Represents a strategy for determining whether to fetch more data based on the current state of the pager.
 * The fetching strategy is responsible for deciding whether to fetch more data based on the anchor position,
 * prefetch position, paging configuration, and the current state of the paging buffer.
 *
 * Implementing a custom [FetchingStrategy] allows you to define your own logic for when to fetch more data.
 * For example, you can fetch more data when the user scrolls near the end of the currently loaded data, or when a certain number of items are remaining in the buffer.
 */
interface FetchingStrategy<Id : Comparable<Id>, K : Any, E: Any> {

    /**
     * Determines whether to fetch more data based on the current state of the pager.
     * The [shouldFetchForward] implementation should determine whether more data should be fetched based on the provided parameters.
     */
    fun shouldFetchForward(
        params: PagingSource.LoadParams<K>,
        pagingState: PagingState<Id, E>,
        fetchingState: FetchingState<Id>,
    ): Boolean

    fun shouldFetchBackward(
        params: PagingSource.LoadParams<K>,
        pagingState: PagingState<Id, E>,
        fetchingState: FetchingState<Id>,
    ): Boolean
}