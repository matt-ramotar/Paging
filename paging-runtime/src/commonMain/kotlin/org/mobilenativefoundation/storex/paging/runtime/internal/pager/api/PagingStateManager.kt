package org.mobilenativefoundation.storex.paging.runtime.internal.pager.api

import kotlinx.coroutines.flow.StateFlow
import org.mobilenativefoundation.storex.paging.runtime.PagingState

/**
 * Manages the paging state and provides methods to update it.
 */
internal interface PagingStateManager<ItemId: Any> {
    val pagingState: StateFlow<PagingState<ItemId>>
    fun updateWithAppendData(ids: List<ItemId?>, endOfPaginationReached: Boolean)
    fun updateWithPrependData(ids: List<ItemId?>, endOfPaginationReached: Boolean)
    fun updateWithAppendError(error: Throwable)
    fun updateWithPrependError(error: Throwable)
    fun updateWithAppendLoading()
    fun updateWithPrependLoading()
}