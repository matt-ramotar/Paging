package org.mobilenativefoundation.storex.paging.runtime.internal.pager.impl

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.mobilenativefoundation.storex.paging.custom.Middleware
import org.mobilenativefoundation.storex.paging.runtime.Action
import org.mobilenativefoundation.storex.paging.runtime.ErrorHandlingStrategy
import org.mobilenativefoundation.storex.paging.runtime.LoadDirection
import org.mobilenativefoundation.storex.paging.runtime.LoadStrategy
import org.mobilenativefoundation.storex.paging.runtime.PagingSource
import org.mobilenativefoundation.storex.paging.runtime.internal.logger.api.PagingLogger
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.api.ExponentialBackoff
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.api.FetchingStateHolder
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.api.LoadingHandler
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.api.OperationApplier
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.api.PagingStateManager
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.api.QueueManager
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.api.RetryBookkeeper
import org.mobilenativefoundation.storex.paging.runtime.internal.store.api.NormalizedStore
import org.mobilenativefoundation.storex.paging.runtime.internal.store.api.PageLoadState

internal class DefaultLoadingHandler<ItemId : Any, PageRequestKey : Any, ItemValue : Any>(
    private val store: NormalizedStore<ItemId, PageRequestKey, ItemValue>,
    private val pagingStateManager: PagingStateManager<ItemId, PageRequestKey, ItemValue>,
    private val queueManager: QueueManager<PageRequestKey>,
    private val fetchingStateHolder: FetchingStateHolder<ItemId, PageRequestKey>,
    private val errorHandlingStrategy: ErrorHandlingStrategy,
    private val middleware: List<Middleware<PageRequestKey>>,
    private val operationApplier: OperationApplier<ItemId, PageRequestKey, ItemValue>,
    private val retryBookkeeper: RetryBookkeeper<ItemId, PageRequestKey>,
    private val logger: PagingLogger,
    private val exponentialBackoff: ExponentialBackoff
) : LoadingHandler<ItemId, PageRequestKey, ItemValue> {

    // Single mutex for all loading operations to prevent race conditions
    // TODO(): Optimize concurrency control by using more fine-grained locking or explore lock-free alternatives for specific operations
    private val loadingMutex = Mutex()

    // Cache middleware results to avoid recomputation
    // TODO(): Optimize middleware caching with size limit or expiration policy to prevent memory issues
    private val middlewareCache = mutableMapOf<PagingSource.LoadParams<PageRequestKey>, PagingSource.LoadParams<PageRequestKey>>()

    override suspend fun handleAppendLoading(loadParams: PagingSource.LoadParams<PageRequestKey>, addNextToQueue: Boolean) =
        handleLoading(loadParams, addNextToQueue)

    override suspend fun handlePrependLoading(loadParams: PagingSource.LoadParams<PageRequestKey>, addNextToQueue: Boolean) =
        handleLoading(loadParams, addNextToQueue)

    private suspend fun handleLoading(loadParams: PagingSource.LoadParams<PageRequestKey>, addNextToQueue: Boolean) {

        logger.debug(
            """
            Trying to load
                Load params: $loadParams
                Direction: ${loadParams.direction}
        """.trimIndent()
        )

        // Using default dispatcher for CPU-intensive tasks
        withContext(Dispatchers.Default) {
            loadingMutex.withLock {
                val modifiedParams = applyMiddleware(loadParams)
                logger.debug(
                    """
                    Applied middleware
                        Provided params: $loadParams
                        Modified params: $modifiedParams
                """.trimIndent()
                )

                updateLoadingState(loadParams.direction)

                // Update max and min request so far for both append and prepend
                // This ensures we're tracking the full range of requests regardless of sort order
                // These methods don't necessarily update the fetching state, but simply recompute the max and min
                fetchingStateHolder.updateMaxRequestSoFar(loadParams.key)
                fetchingStateHolder.updateMinRequestSoFar(loadParams.key)

                tryLoad(loadParams, addNextToQueue)
            }
        }
    }

    // This is extracted from handleLoading because we might need to retry the load
    // We can't simply re invoke handleLoading is because it will cause a deadlock
    private suspend fun tryLoad(loadParams: PagingSource.LoadParams<PageRequestKey>, addNextToQueue: Boolean) {
        try {

            loadPage(loadParams, addNextToQueue)
        } catch (pagingError: Throwable) {
            logger.error(
                """
                        Caught error
                            Load params: $loadParams
                            Direction: ${loadParams.direction}
                    """.trimIndent(),
                pagingError
            )
            handleError(pagingError, loadParams, addNextToQueue)
        }
    }

    private suspend fun loadPage(loadParams: PagingSource.LoadParams<PageRequestKey>, addNextToQueue: Boolean) {
        val pageLoadFlow = store.loadPage(loadParams)

        // Design decision to use only the latest data for any given params
        // Not OK to skip params, but OK to skip stale data
        pageLoadFlow.collectLatest { pageLoadState ->
            when (pageLoadState) {
                is PageLoadState.Success -> handleSuccess(pageLoadState, loadParams.key, loadParams.direction, addNextToQueue)
                is PageLoadState.Error -> throw pageLoadState.toThrowable()
                else -> { /* Do nothing for other states */
                }
            }
        }
    }

    private suspend fun applyMiddleware(params: PagingSource.LoadParams<PageRequestKey>): PagingSource.LoadParams<PageRequestKey> {

        // Caching middleware results to avoid recomputations for the same parameters
        return middlewareCache.getOrPut(params) {
            middleware.fold(params) { acc, mw -> mw.apply(acc) { it } }
        }
    }

    // Inlining for better performance
    private inline fun updateLoadingState(direction: LoadDirection) {
        when (direction) {
            LoadDirection.Append -> pagingStateManager.updateWithAppendLoading()
            LoadDirection.Prepend -> pagingStateManager.updateWithPrependLoading()
        }
    }

    private suspend fun handleSuccess(
        successState: PageLoadState.Success<ItemId, PageRequestKey, ItemValue>,
        key: PageRequestKey,
        direction: LoadDirection,
        addNextToQueue: Boolean
    ) {
        logger.debug(
            """
            Applying operations to data
                Key: $key
                Direction: $direction
        """.trimIndent()
        )

        val transformedData = operationApplier.applyOperations(
            successState.snapshot,
            key,
            pagingStateManager.pagingState.value,
            fetchingStateHolder.state.value
        )

        // Using a when expression to minimize object creation
        when (direction) {
            LoadDirection.Append -> {
                pagingStateManager.updateWithAppendData(transformedData.getAllIds(), successState.nextKey == null)
                if (addNextToQueue) {
                    successState.nextKey?.let { enqueueNext(it, LoadDirection.Append) }
                }
            }

            LoadDirection.Prepend -> {
                pagingStateManager.updateWithPrependData(transformedData.getAllIds(), successState.prevKey == null)
                if (addNextToQueue) {
                    successState.prevKey?.let { enqueueNext(it, LoadDirection.Prepend) }
                }
            }
        }

        queueManager.updateExistingPendingJob(key, inFlight = false, completed = true)
    }

    private suspend fun passThroughError(error: Throwable, loadParams: PagingSource.LoadParams<PageRequestKey>) {

        // Passing the error through
        updateErrorState(error, loadParams.direction)

        // Complete the job
        completeLoadJobAfterError(loadParams)
    }

    private suspend fun completeLoadJobAfterError(loadParams: PagingSource.LoadParams<PageRequestKey>) {
        // Design decision to remove placeholders when not retrying after an error
        // Page refreshes are not currently supported
        // So the page must currently contain placeholders
        store.clearPage(loadParams.key)

        // Marking the pending job as completed
        queueManager.updateExistingPendingJob(loadParams.key, inFlight = false, completed = true)

        when (loadParams.direction) {
            LoadDirection.Prepend -> {
                // Removing the params from the prepend queue
                queueManager.prependQueue.removeFirst { it.params == loadParams }
            }

            LoadDirection.Append -> {
                // Removing the params from the append queue
                queueManager.appendQueue.removeFirst { it.params == loadParams }
            }
        }

        // Resetting retries for these params
        retryBookkeeper.resetCount(loadParams)
    }

    private suspend fun handleError(error: Throwable, loadParams: PagingSource.LoadParams<PageRequestKey>, addNextToQueue: Boolean) {
        when (errorHandlingStrategy) {
            ErrorHandlingStrategy.Ignore -> {
                logger.error(
                    """
                    Ignoring the error
                        Load params: $loadParams
                        Direction: ${loadParams.direction}
                """.trimIndent(),
                    error
                )

                // Complete the job, do nothing else
                completeLoadJobAfterError(loadParams)
            }

            ErrorHandlingStrategy.PassThrough -> {
                logger.error(
                    """
                    Passing the error through
                        Load params: $loadParams
                        Direction: $loadParams.direction
                """.trimIndent(),
                    error
                )
                passThroughError(error, loadParams)
            }

            is ErrorHandlingStrategy.RetryLast -> {

                val retryCount = retryBookkeeper.getCount(loadParams)

                logger.debug(
                    """
                        Determining whether to retry
                            Load params: $loadParams
                            Direction: ${loadParams.direction}
                            Retry count: $retryCount
                            Max retries: ${errorHandlingStrategy.maxRetries}
                    """.trimIndent()
                )

                if (retryCount < errorHandlingStrategy.maxRetries) {

                    // Incrementing retry count
                    retryBookkeeper.incrementCount(loadParams)

                    // Retrying with backoff
                    exponentialBackoff.execute(retryCount) {
                        when (loadParams.direction) {
                            LoadDirection.Prepend -> {
                                logger.verbose("Retrying prepend load for: $loadParams")
                                tryLoad(loadParams, addNextToQueue)
                            }

                            LoadDirection.Append -> {
                                logger.verbose("Retrying append load for: $loadParams")
                                tryLoad(loadParams, addNextToQueue)
                            }
                        }
                    }

                } else {
                    logger.verbose("At maximum retries, passing the error through")
                    passThroughError(error, loadParams)
                }
            }
        }
    }

    // Inlining for better performance
    private inline fun updateErrorState(error: Throwable, direction: LoadDirection) {
        when (direction) {
            LoadDirection.Append -> pagingStateManager.updateWithAppendError(error)
            LoadDirection.Prepend -> pagingStateManager.updateWithPrependError(error)
        }
    }

    private suspend fun enqueueNext(key: PageRequestKey, direction: LoadDirection) {
        // Skip cache because these params come from a network response
        val action = Action.Enqueue(key, direction, LoadStrategy.SkipCache, jump = false)
        logger.debug("Enqueuing next action: $action")
        when (direction) {
            LoadDirection.Append -> queueManager.enqueueAppend(action)
            LoadDirection.Prepend -> queueManager.enqueuePrepend(action)
        }
    }

    private fun PageLoadState.Error<*, *, *>.toThrowable(): Throwable = when (this) {
        is PageLoadState.Error.Exception -> error
        is PageLoadState.Error.Message -> Exception(error)
    }
}