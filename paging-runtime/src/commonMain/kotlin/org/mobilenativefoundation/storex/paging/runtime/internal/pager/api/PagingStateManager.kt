package org.mobilenativefoundation.storex.paging.runtime.internal.pager.api

import kotlinx.coroutines.flow.StateFlow
import org.mobilenativefoundation.storex.paging.runtime.Identifier
import org.mobilenativefoundation.storex.paging.runtime.PagingState

/**
 * Manages the paging state and provides methods to update it.
 */
internal interface PagingStateManager<Id : Identifier<Id>> {
    val pagingState: StateFlow<PagingState<Id>>
    fun updateWithAppendData(ids: List<Id?>, endOfPaginationReached: Boolean)
    fun updateWithPrependData(ids: List<Id?>, endOfPaginationReached: Boolean)
    fun updateWithAppendError(error: Throwable)
    fun updateWithPrependError(error: Throwable)
    fun updateWithAppendLoading()
    fun updateWithPrependLoading()
}