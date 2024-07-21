package org.mobilenativefoundation.storex.paging.runtime.internal.pager.impl

import kotlinx.coroutines.flow.first
import org.mobilenativefoundation.storex.paging.custom.Middleware
import org.mobilenativefoundation.storex.paging.runtime.Action
import org.mobilenativefoundation.storex.paging.runtime.ErrorHandlingStrategy
import org.mobilenativefoundation.storex.paging.runtime.LoadDirection
import org.mobilenativefoundation.storex.paging.runtime.LoadStrategy
import org.mobilenativefoundation.storex.paging.runtime.PagingSource
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.api.FetchingStateHolder
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.api.LoadingHandler
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.api.OperationApplier
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.api.PagingStateManager
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.api.QueueManager
import org.mobilenativefoundation.storex.paging.runtime.internal.store.api.NormalizedStore
import org.mobilenativefoundation.storex.paging.runtime.internal.store.api.PageLoadState


/**
 * Implementation of LoadingHandler that manages loading operations.
 */
internal class RealLoadingHandler<ItemId: Any, PageRequestKey: Any, ItemValue: Any>(
    private val store: NormalizedStore<ItemId, PageRequestKey, ItemValue>,
    private val pagingStateManager: PagingStateManager<ItemId>,
    private val queueManager: QueueManager<PageRequestKey>,
    private val fetchingStateHolder: FetchingStateHolder<ItemId, PageRequestKey>,
    private val errorHandlingStrategy: ErrorHandlingStrategy,
    private val middleware: List<Middleware<PageRequestKey>>,
    private val operationApplier: OperationApplier<ItemId, PageRequestKey, ItemValue>
) : LoadingHandler<ItemId, PageRequestKey, ItemValue> {

    override suspend fun handleAppendLoading(
        loadParams: PagingSource.LoadParams<PageRequestKey>,
        addNextToQueue: Boolean
    ) {
        val modifiedParams = applyMiddleware(loadParams)

        pagingStateManager.updateWithAppendLoading()
        try {
            fetchingStateHolder.updateMaxRequestSoFar(modifiedParams.key)
            loadAppendPage(modifiedParams, addNextToQueue)
        } catch (pagingError: Throwable) {
            handleAppendPagingError(pagingError, modifiedParams)
        }
    }


    override suspend fun handlePrependLoading(loadParams: PagingSource.LoadParams<PageRequestKey>, addNextToQueue: Boolean) {
        val modifiedParams = applyMiddleware(loadParams)

        pagingStateManager.updateWithPrependLoading()
        try {
            loadPrependPage(modifiedParams)
        } catch (pagingError: Throwable) {
            handlePrependPagingError(pagingError, modifiedParams)
        }
    }

    private suspend fun loadAppendPage(
        loadParams: PagingSource.LoadParams<PageRequestKey>,
        addNextToQueue: Boolean
    ) {
        store.loadPage(loadParams).first { pageLoadState ->
            handleAppendPageLoadState(pageLoadState, loadParams.key, addNextToQueue)
        }
    }

    private suspend fun loadPrependPage(loadParams: PagingSource.LoadParams<PageRequestKey>) {
        store.loadPage(loadParams).first { pageLoadState ->
            handlePrependPageLoadState(pageLoadState, loadParams.key)
        }
    }

    private suspend fun handleAppendPageLoadState(
        pageLoadState: PageLoadState<ItemId, PageRequestKey, ItemValue>,
        key: PageRequestKey,
        addNextToQueue: Boolean
    ): Boolean {
        return when (pageLoadState) {
            is PageLoadState.Success -> handleAppendSuccess(pageLoadState, key, addNextToQueue)
            is PageLoadState.Error -> throw Throwable(pageLoadState.toString())
            else -> false
        }
    }

    private suspend fun handlePrependPageLoadState(
        pageLoadState: PageLoadState<ItemId, PageRequestKey, ItemValue>,
        key: PageRequestKey
    ): Boolean {
        return when (pageLoadState) {
            is PageLoadState.Success -> handlePrependSuccess(pageLoadState, key)
            is PageLoadState.Error -> throw Throwable(pageLoadState.toString())
            else -> false
        }
    }

    private suspend fun handleAppendSuccess(
        successState: PageLoadState.Success<ItemId, PageRequestKey, ItemValue>,
        key: PageRequestKey,
        addNextToQueue: Boolean
    ): Boolean {

        val transformedData = operationApplier.applyOperations(
            successState.snapshot,
            key,
            pagingStateManager.pagingState.value,
            fetchingStateHolder.state.value
        )

        pagingStateManager.updateWithAppendData(
            transformedData.getAllIds(),
            successState.nextKey == null
        )
        if (addNextToQueue) {
            successState.nextKey?.let { enqueueAppendNext(it) }
        }
        queueManager.updateExistingPendingJob(key, inFlight = false, completed = true)
        return true
    }

    private suspend fun handlePrependSuccess(
        successState: PageLoadState.Success<ItemId, PageRequestKey, ItemValue>,
        key: PageRequestKey
    ): Boolean {

        val transformedData = operationApplier.applyOperations(
            successState.snapshot,
            key,
            pagingStateManager.pagingState.value,
            fetchingStateHolder.state.value
        )

        pagingStateManager.updateWithPrependData(
            transformedData.getAllIds(),
            successState.prevKey == null
        )
        successState.prevKey?.let { enqueuePrependNext(it) }
        queueManager.updateExistingPendingJob(key, inFlight = false, completed = true)
        return true
    }

    private fun handleAppendPagingError(
        pagingError: Throwable,
        loadParams: PagingSource.LoadParams<PageRequestKey>
    ) {
        when (errorHandlingStrategy) {
            ErrorHandlingStrategy.Ignore -> {
                // Ignore the error
            }

            ErrorHandlingStrategy.PassThrough -> {
                pagingStateManager.updateWithAppendError(pagingError)
            }

            is ErrorHandlingStrategy.RetryLast -> {
                // Implement retry logic
            }
        }
    }

    private fun handlePrependPagingError(
        pagingError: Throwable,
        loadParams: PagingSource.LoadParams<PageRequestKey>
    ) {
        when (errorHandlingStrategy) {
            ErrorHandlingStrategy.Ignore -> {
                // Ignore the error
            }

            ErrorHandlingStrategy.PassThrough -> {
                pagingStateManager.updateWithPrependError(pagingError)
            }

            is ErrorHandlingStrategy.RetryLast -> {
                // Implement retry logic
            }
        }
    }

    private suspend fun enqueueAppendNext(key: PageRequestKey) {
        queueManager.enqueueAppend(
            Action.Enqueue(
                key = key,
                direction = LoadDirection.Append,
                strategy = LoadStrategy.SkipCache,
                jump = false
            )
        )
    }

    private suspend fun enqueuePrependNext(key: PageRequestKey) {
        queueManager.enqueuePrepend(
            Action.Enqueue(
                key = key,
                direction = LoadDirection.Prepend,
                strategy = LoadStrategy.SkipCache,
                jump = false
            )
        )
    }

    private suspend fun applyMiddleware(initialParams: PagingSource.LoadParams<PageRequestKey>): PagingSource.LoadParams<PageRequestKey> {
        suspend fun applyRemaining(
            index: Int,
            params: PagingSource.LoadParams<PageRequestKey>
        ): PagingSource.LoadParams<PageRequestKey> {
            if (index >= middleware.size) {
                return params
            }
            return middleware[index].apply(params) { nextParams ->
                applyRemaining(index + 1, nextParams)
            }
        }

        return applyRemaining(0, initialParams)
    }
}