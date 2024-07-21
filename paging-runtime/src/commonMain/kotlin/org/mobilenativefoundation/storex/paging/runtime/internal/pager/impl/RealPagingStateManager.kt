package org.mobilenativefoundation.storex.paging.runtime.internal.pager.impl

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.mobilenativefoundation.storex.paging.runtime.CombinedLoadStates
import org.mobilenativefoundation.storex.paging.runtime.LoadState
import org.mobilenativefoundation.storex.paging.runtime.PagingState
import org.mobilenativefoundation.storex.paging.runtime.internal.logger.api.PagingLogger
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.api.PagingStateManager


/**
 * Implementation of PagingStateManager that manages the paging state.
 */
class RealPagingStateManager<ItemId : Any, PageRequestKey : Any, ItemValue : Any>(
    initialState: PagingState<ItemId, PageRequestKey, ItemValue>,
    private val logger: PagingLogger,
) : PagingStateManager<ItemId, PageRequestKey, ItemValue> {
    private val _pagingState = MutableStateFlow(initialState)
    override val pagingState: StateFlow<PagingState<ItemId, PageRequestKey, ItemValue>> = _pagingState.asStateFlow()

    override fun updateWithAppendData(ids: List<ItemId?>, endOfPaginationReached: Boolean) {
        updateState("append data") { currentState ->
            currentState.copy(
                ids = currentState.ids + ids,
                loadStates = currentState.loadStates.copy(
                    append = LoadState.NotLoading(endOfPaginationReached)
                )
            )
        }
    }

    override fun updateWithPrependData(ids: List<ItemId?>, endOfPaginationReached: Boolean) {
        updateState("prepend data") { currentState ->
            currentState.copy(
                ids = ids + currentState.ids,
                loadStates = currentState.loadStates.copy(
                    prepend = LoadState.NotLoading(endOfPaginationReached)
                )
            )
        }
    }

    override fun updateWithAppendError(error: Throwable) {
        updateLoadState("append error") { it.copy(append = LoadState.Error(error)) }
    }

    override fun updateWithPrependError(error: Throwable) {
        updateLoadState("prepend error") { it.copy(prepend = LoadState.Error(error)) }
    }

    override fun updateWithAppendLoading() {
        updateLoadState("append loading") { it.copy(append = LoadState.Loading) }
    }

    override fun updateWithPrependLoading() {
        updateLoadState("prepend loading") { it.copy(prepend = LoadState.Loading) }
    }

    private fun updateState(
        operation: String,
        update: (PagingState<ItemId, PageRequestKey, ItemValue>) -> PagingState<ItemId, PageRequestKey, ItemValue>
    ) {
        logger.debug("Updating paging state with $operation")
        logger.debug("Current paging state: ${pagingState.value}")

        _pagingState.update(update)

        logger.debug("Updated paging state: ${pagingState.value}")
    }

    private fun updateLoadState(operation: String, update: (CombinedLoadStates) -> CombinedLoadStates) {
        updateState(operation) { currentState ->
            currentState.copy(loadStates = update(currentState.loadStates))
        }
    }
}