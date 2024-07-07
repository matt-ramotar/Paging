package org.mobilenativefoundation.storex.paging.runtime.internal.pager.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import app.cash.molecule.launchMolecule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.mobilenativefoundation.storex.paging.custom.FetchingStrategy
import org.mobilenativefoundation.storex.paging.custom.LaunchEffect
import org.mobilenativefoundation.storex.paging.runtime.Action
import org.mobilenativefoundation.storex.paging.runtime.FetchingState
import org.mobilenativefoundation.storex.paging.runtime.Identifiable
import org.mobilenativefoundation.storex.paging.runtime.Identifier
import org.mobilenativefoundation.storex.paging.runtime.LoadDirection
import org.mobilenativefoundation.storex.paging.runtime.Pager
import org.mobilenativefoundation.storex.paging.runtime.PagingConfig
import org.mobilenativefoundation.storex.paging.runtime.PagingSource
import org.mobilenativefoundation.storex.paging.runtime.PagingState
import org.mobilenativefoundation.storex.paging.runtime.RecompositionMode
import org.mobilenativefoundation.storex.paging.runtime.internal.logger.api.PagingLogger
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.api.FetchingStateHolder
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.api.LoadParamsQueue
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.api.LoadingHandler
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.api.PagingStateManager
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.api.QueueManager
import org.mobilenativefoundation.storex.paging.runtime.internal.store.api.NormalizedStore


/**
 * Implementation of [Pager] that coordinates paging operations.

 */
internal class RealPager<Id : Identifier<Id>, K : Comparable<K>, V : Identifiable<Id>>(
    recompositionMode: RecompositionMode,
    private val fetchingStateHolder: FetchingStateHolder<Id, K>,
    private val launchEffects: List<LaunchEffect>,
    private val fetchingStrategy: FetchingStrategy<Id, K>,
    private val initialLoadParams: PagingSource.LoadParams<K>,
    private val store: NormalizedStore<Id, K, V>,
    private val actions: Flow<Action<K>>,
    private val logger: PagingLogger,
    private val pagingStateManager: PagingStateManager<Id>,
    private val queueManager: QueueManager<K>,
    private val loadingHandler: LoadingHandler<Id, K, V>,
    private val coroutineScope: CoroutineScope,
) : Pager<Id> {

    init {

        logger.debug("Initializing RealPager")

        handleLaunchEffects()
        handleEagerLoading()
    }

    override val flow: Flow<PagingState<Id>> =
        coroutineScope.launchMolecule(recompositionMode.toCash()) {
            pagingState(actions)
        }

    /**
     * Composable function that manages the paging state.
     * @param actions Flow of [Action] objects.
     */
    @Composable
    private fun pagingState(actions: Flow<Action<K>>): PagingState<Id> {
        val fetchingState by fetchingStateHolder.state.collectAsState()
        val pagingState by pagingStateManager.pagingState.collectAsState()

        logger.debug("Recomposing")
        logger.debug("Current fetching state: $fetchingState")
        logger.debug("Current paging state: $pagingState")

        LaunchedEffect(Unit) {
            handleActions(actions)
        }

        LaunchedEffect(fetchingState) {
            handlePrefetching()
        }

        LaunchedEffect(fetchingState.minItemAccessedSoFar) {
            processPrependQueue()
        }

        LaunchedEffect(fetchingState.maxItemAccessedSoFar) {
            processAppendQueue()
        }

        return pagingState
    }


    /**
     * Handles launch effects.
     */
    private fun handleLaunchEffects() {
        logger.debug("Handling launch effects")

        coroutineScope.launch {
            launchEffects.forEach {
                logger.debug("Invoking launch effect")
                it.invoke()
            }
        }
    }

    /**
     * Handles eager loading on initialization.
     */
    private fun handleEagerLoading() {
        logger.debug("Handling eager loading with initial key: ${initialLoadParams.key}")

        coroutineScope.launch {
            queueManager.enqueueAppend(
                Action.Enqueue(
                    key = initialLoadParams.key,
                    strategy = initialLoadParams.strategy,
                    direction = LoadDirection.Append,
                    jump = false
                )
            )

            processAppendQueue()
        }
    }

    /**
     * Handles incoming paging actions.
     *
     * @param actions Flow of paging actions.
     */
    private suspend fun handleActions(actions: Flow<Action<K>>) {
        actions.distinctUntilChanged().collect { action ->
            logger.debug("Handling action: $action")

            when (action) {
                is Action.ProcessQueue -> handleProcessQueueAction(action)
                is Action.SkipQueue -> handleSkipQueueAction(action)
                is Action.Enqueue -> handleEnqueueAction(action)
                Action.Invalidate -> handleInvalidateAction()
            }
        }
    }

    /**
     * Handles prefetching for both append and prepend directions.
     */
    private suspend fun handlePrefetching() {
        logger.debug("Handling prefetching")

        processAppendQueue()
        processPrependQueue()
    }

    /**
     * Handles a process queue action.
     *
     * @param action The process queue action.
     */
    private suspend fun handleProcessQueueAction(action: Action.ProcessQueue) {
        logger.debug("Handling ProcessQueue action: $action")

        when (action.direction) {
            LoadDirection.Prepend -> processPrependQueue()
            LoadDirection.Append -> processAppendQueue()
        }
    }

    /**
     * Handles a skip queue action.
     *
     * @param action The skip queue action.
     */
    private suspend fun handleSkipQueueAction(action: Action.SkipQueue<K>) {
        logger.debug("Handling SkipQueue action: $action")

        queueManager.addPendingJob(action.key, inFlight = true)

        when (action.direction) {
            LoadDirection.Prepend -> loadingHandler.handlePrependLoading(action.toPagingSourceLoadParams())
            LoadDirection.Append -> loadingHandler.handleAppendLoading(
                action.toPagingSourceLoadParams(),
                addNextToQueue = false
            )
        }
    }

    /**
     * Handles an enqueue action.
     *
     * @param action The enqueue action.
     */
    private suspend fun handleEnqueueAction(action: Action.Enqueue<K>) {
        logger.debug("Handling Enqueue action: $action")

        when (action.direction) {
            LoadDirection.Prepend -> queueManager.enqueuePrepend(action)
            LoadDirection.Append -> queueManager.enqueueAppend(action)
        }
    }

    /**
     * Handles an invalidate action.
     */
    private suspend fun handleInvalidateAction() {
        logger.debug("Handling Invalidate action")

        queueManager.clearQueues()
        store.invalidateAll()
    }

    /**
     * Processes the prepend queue.
     */
    private suspend fun processPrependQueue() {
        logger.debug("Processing prepend queue")
        while (queueManager.prependQueue.isNotEmpty()) {
            val lastQueueElement = queueManager.prependQueue.removeLast()
            loadingHandler.handlePrependLoading(lastQueueElement.params)
        }
    }


    /**
     * Processes the append queue.
     */
    private suspend fun processAppendQueue() {
        logger.debug("Processing prepend queue")

        var keepFetching = true
        while (queueManager.appendQueue.isNotEmpty() && keepFetching) {
            if (queueManager.hasPendingJobs()) {
                logger.debug("Pending jobs exist, delaying append processing")
                delay(100)
                continue
            }

            val pagingState = pagingStateManager.pagingState.value
            val fetchingState = fetchingStateHolder.state.value
            val firstQueueElement = queueManager.appendQueue.first()

            if (shouldFetchForward(firstQueueElement, pagingState, fetchingState)) {
                logger.debug("Fetching forward for key: ${firstQueueElement.params.key}")

                val queueElement = queueManager.appendQueue.removeFirst()
                loadingHandler.handleAppendLoading(queueElement.params)
            } else {
                logger.debug("Stopping forward fetch")

                keepFetching = false
            }
        }
    }

    /**
     * Determines if we should fetch more data in the forward direction.
     *
     * @param queueElement The queue element to consider.
     * @param pagingState The current paging state.
     * @param fetchingState The current fetching state.
     * @return True if we should fetch more data, false otherwise.
     */
    private fun shouldFetchForward(
        queueElement: LoadParamsQueue.Element<K>,
        pagingState: PagingState<Id>,
        fetchingState: FetchingState<Id, K>
    ): Boolean {
        val shouldFetch =
            queueElement.mechanism == LoadParamsQueue.Element.Mechanism.EnqueueRequest ||
                    fetchingStrategy.shouldFetchForward(
                        queueElement.params,
                        pagingState,
                        fetchingState
                    )

        logger.debug("Should fetch forward: $shouldFetch for key: ${queueElement.params.key}")

        return shouldFetch
    }


    /**
     * Converts [Action.SkipQueue] to [PagingSource.LoadParams].
     */
    private fun Action.SkipQueue<K>.toPagingSourceLoadParams(): PagingSource.LoadParams<K> {
        return PagingSource.LoadParams(
            key = key,
            strategy = strategy,
            direction = direction
        )
    }
}