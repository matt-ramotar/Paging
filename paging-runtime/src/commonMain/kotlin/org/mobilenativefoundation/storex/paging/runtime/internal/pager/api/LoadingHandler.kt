package org.mobilenativefoundation.storex.paging.runtime.internal.pager.api

import org.mobilenativefoundation.storex.paging.runtime.PagingSource

/**
 * Handles loading operations for both append and prepend directions.
 */
internal interface LoadingHandler<ItemId: Any, PageRequestKey: Any, ItemValue: Any> {
    suspend fun handleAppendLoading(
        loadParams: PagingSource.LoadParams<PageRequestKey>,
        addNextToQueue: Boolean = true
    )

    suspend fun handlePrependLoading(loadParams: PagingSource.LoadParams<PageRequestKey>, addNextToQueue: Boolean = true)
}