package org.mobilenativefoundation.storex.paging.runtime.internal.pager.impl

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.mobilenativefoundation.storex.paging.runtime.Identifier
import org.mobilenativefoundation.storex.paging.runtime.LoadState
import org.mobilenativefoundation.storex.paging.runtime.PagingState
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.api.PagingStateManager


/**
 * Implementation of PagingStateManager that manages the paging state.
 */
class RealPagingStateManager<Id : Identifier<Id>>(initialState: PagingState<Id>) :
    PagingStateManager<Id> {
    private val _pagingState = MutableStateFlow(initialState)
    override val pagingState: StateFlow<PagingState<Id>> = _pagingState.asStateFlow()

    override fun updateWithAppendData(ids: List<Id?>, endOfPaginationReached: Boolean) {
        _pagingState.update { currentState ->
            currentState.copy(
                ids = currentState.ids + ids,
                loadStates = currentState.loadStates.copy(
                    append = LoadState.NotLoading(endOfPaginationReached)
                )
            )
        }
    }

    override fun updateWithPrependData(ids: List<Id?>, endOfPaginationReached: Boolean) {
        _pagingState.update { currentState ->
            currentState.copy(
                ids = ids + currentState.ids,
                loadStates = currentState.loadStates.copy(
                    prepend = LoadState.NotLoading(endOfPaginationReached)
                )
            )
        }
    }

    override fun updateWithAppendError(error: Throwable) {
        _pagingState.update { currentState ->
            currentState.copy(
                loadStates = currentState.loadStates.copy(
                    append = LoadState.Error(error)
                )
            )
        }
    }

    override fun updateWithPrependError(error: Throwable) {
        _pagingState.update { currentState ->
            currentState.copy(
                loadStates = currentState.loadStates.copy(
                    prepend = LoadState.Error(error)
                )
            )
        }
    }

    override fun updateWithAppendLoading() {
        _pagingState.update { currentState ->
            currentState.copy(
                loadStates = currentState.loadStates.copy(
                    append = LoadState.Loading
                )
            )
        }
    }

    override fun updateWithPrependLoading() {
        _pagingState.update { currentState ->
            currentState.copy(
                loadStates = currentState.loadStates.copy(
                    prepend = LoadState.Loading
                )
            )
        }
    }
}