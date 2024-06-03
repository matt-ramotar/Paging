package org.mobilenativefoundation.storex.paging.internal.impl

import androidx.compose.runtime.*
import app.cash.molecule.launchMolecule
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.serializer
import org.mobilenativefoundation.storex.paging.*
import org.mobilenativefoundation.storex.paging.custom.*
import org.mobilenativefoundation.storex.paging.internal.api.FetchingStateHolder
import org.mobilenativefoundation.storex.paging.internal.api.NormalizedStore

// TODO(): Design decision to support initial state (e.g., hardcoded)

@OptIn(InternalSerializationApi::class)
class RealPager<Id : Comparable<Id>, Q : Quantifiable<Id>, K : Comparable<K>, V : Identifiable<Id, Q>, E : Any, P : Any>(
    coroutineDispatcher: CoroutineDispatcher,
    private val fetchingStateHolder: FetchingStateHolder<Id, Q, K>,
    private val launchEffects: List<LaunchEffect>,
    private val errorHandlingStrategy: ErrorHandlingStrategy,
    private val middleware: List<Middleware<K>>,
    private val fetchingStrategy: FetchingStrategy<Id, Q, K, E>,
    private val initialLoadParams: PagingSource.LoadParams<K>,
    private val registry: KClassRegistry<Id, Q, K, V, E>,
    private val normalizedStore: NormalizedStore<Id, Q, K, V, E>,
    private val operations: List<Operation<Id, Q, K, V, P, P>>,
    private val initialState: PagingState<Id, Q, E> = PagingState.initial()
) : Pager<Id, Q, K, V, E> {

    private val coroutineScope = CoroutineScope(coroutineDispatcher)

    // TODO(): This is not thread safe!
    private val _mutablePagingState = MutableStateFlow(initialState)
    private val _processingAppendQueue = MutableStateFlow(false)

    private val pendingSkipQueueJobs = mutableMapOf<K, PendingSkipQueueJob<K>>()

    data class PendingSkipQueueJob<K : Any>(
        val key: K,
        val inFlight: Boolean,
    )

    private val appendLoadParamsQueue: LoadParamsQueue<K> = LoadParamsQueue()
    private val prependLoadParamsQueue: LoadParamsQueue<K> = LoadParamsQueue()


    init {
        handleLaunchEffects()
        handleEagerLoading()
    }

    override fun createSelfUpdatingItem(id: Q): SelfUpdatingItem<Id, Q, V, E> {
        println("SELF UPDATING ITEM CALLED")
        return normalizedStore.selfUpdatingItem(id)
    }

    private fun PagingRequest.Enqueue<K>.toPagingSourceLoadParams(): PagingSource.LoadParams<K> {
        return PagingSource.LoadParams(
            key = key,
            strategy = strategy,
            direction = direction
        )
    }

    private fun PagingRequest.SkipQueue<K>.toPagingSourceLoadParams(): PagingSource.LoadParams<K> {
        return PagingSource.LoadParams(
            key = key,
            strategy = strategy,
            direction = direction
        )
    }

    override fun pagingFlow(
        requests: Flow<PagingRequest<K>>,
        recompositionMode: RecompositionMode
    ): Flow<PagingState<Id, Q, E>> =
        coroutineScope.launchMolecule(recompositionMode.toCashRecompositionMode()) {
            pagingState(requests)
        }

    override fun pagingStateFlow(
        composableCoroutineScope: CoroutineScope,
        requests: Flow<PagingRequest<K>>
    ): StateFlow<PagingState<Id, Q, E>> {

        println("*** BEFORE LAUNCHING MOLECULE")
        return composableCoroutineScope.launchMolecule(RecompositionMode.ContextClock.toCashRecompositionMode()) {
            pagingState(requests)
        }
    }

    // TODO(): This is not efficient - we should extract the common logic from [pagingState] - we shouldn't be getting ids and then remapping to values when we have values initially
    override fun pagingItems(coroutineScope: CoroutineScope, requests: Flow<PagingRequest<K>>): StateFlow<List<V>> {
        return coroutineScope.launchMolecule(RecompositionMode.ContextClock.toCashRecompositionMode()) {
            pagingState(requests).ids.mapNotNull { id -> id?.let { normalizedStore.getItem(it) } }
        }
    }


    @Composable
    private fun pagingState(requests: Flow<PagingRequest<K>>): PagingState<Id, Q, E> {
        val fetchingState by fetchingStateHolder.state.collectAsState()

        val pagingState by _mutablePagingState.collectAsState()

        println("TAG = ${pagingState.loadStates.append}")
        println("TAG= ${pagingState.ids}")

        val scope = rememberCoroutineScope()

        LaunchedEffect(Unit) {

            println("LAUNCHING 1")

            requests.distinctUntilChanged().collect { request ->
                println("*** RECEIVED REQUEST")
                when (request) {
                    is PagingRequest.ProcessQueue -> {
                        when (request.direction) {
                            LoadDirection.Prepend -> {
                                processPrependQueue()
                            }

                            LoadDirection.Append -> {
                                processAppendQueue()
                            }
                        }
                    }

                    is PagingRequest.SkipQueue -> {
                        println("*** SKIP QUEUE HITTING HERE")
                        println("*** ${request.key}")

                        when (request.direction) {
                            LoadDirection.Prepend -> {

                                if (pendingSkipQueueJobs[request.key]?.inFlight == true) {
                                    // No op, this is a refresh request already in flight
                                } else {

                                addPendingQueueJob(request.key)
                                handlePrependLoading(request.toPagingSourceLoadParams())
                                }


                            }

                            // We don't add next to queue, because we are only agreeing to skip the queue for this key
                            LoadDirection.Append -> {
                                addPendingQueueJob(request.key)

                                handleAppendLoading(
                                    request.toPagingSourceLoadParams(),
                                    addNextToQueue = false
                                )
                            }
                        }
                    }

                    is PagingRequest.Enqueue -> {
                        when (request.direction) {
                            LoadDirection.Prepend -> {
                                val queueElement = LoadParamsQueueElement(
                                    params = request.toPagingSourceLoadParams(),
                                    mechanism = LoadParamsQueueElement.Mechanism.EnqueueRequest
                                )
                                prependLoadParamsQueue.addLast(queueElement)
                                processPrependQueue()
                            }

                            LoadDirection.Append -> {
                                val queueElement = LoadParamsQueueElement(
                                    params = request.toPagingSourceLoadParams(),
                                    mechanism = LoadParamsQueueElement.Mechanism.EnqueueRequest
                                )

                                if (request.jump) {
                                    println("HITTING IN JUMP")
                                    appendLoadParamsQueue.jump(queueElement)
                                } else {
                                    appendLoadParamsQueue.addLast(queueElement)
                                }
                                println("APPENDED ENQUEUE REQUEST, ABOUT TO PROCESS APPEND QUEUE")
                                processAppendQueue()
                            }
                        }
                    }

                    PagingRequest.Invalidate -> {
                        pendingSkipQueueJobs.clear()
                        appendLoadParamsQueue.clear()
                        prependLoadParamsQueue.clear()

                        normalizedStore.invalidate()
                    }
                }
            }
            println("END OF LAUNCHED EFFECT")
        }

//        LaunchedEffect(fetchingState) {
//            println("LAUNCHING 2")
//
//            handleForwardPrefetching()
//            handleBackwardPrefetching()
//        }
//
//        LaunchedEffect(fetchingState.minItemAccessedSoFar?.value) {
//            println("&&&& - FETCHING STATE MIN ITEM ACCESSED CHANGED ${fetchingState.minItemAccessedSoFar?.value}")
//            processPrependQueue()
//        }

        LaunchedEffect(fetchingState.maxItemAccessedSoFar?.value) {
            println("&&&& - FETCHING STATE MAX ITEM ACCESSED CHANGED ${fetchingState.maxItemAccessedSoFar?.value}")
            processAppendQueue()
        }


        println("RETURNING PAGING STATE ${pagingState.ids.size}")
        return pagingState
    }


    // TODO(): Design decision to get the most recent prepend load params
    private fun processPrependQueue() {

        while (prependLoadParamsQueue.isNotEmpty()) {
            val lastQueueElement = prependLoadParamsQueue.removeLast()

            // TODO(): Design decision not to check whether to fetch for prepend loads

            // TODO(): Design decision not to prepend placeholders

            // Apply middleware only once, if any
            coroutineScope.launch {
                handlePrependLoading(lastQueueElement.params)
            }
        }
    }


    private fun transformSnapshot(
        snapshot: ItemSnapshotList<Id, Q, V>,
        key: K?,
    ): ItemSnapshotList<Id, Q, V> {

        if (operations.isEmpty()) {
            return snapshot
        }

        val pagingState = _mutablePagingState.value
        val fetchingState = fetchingStateHolder.state.value

        return operations.fold(snapshot) { acc, operation ->
            operation.shouldApply(key, pagingState, fetchingState)?.let { params ->
                operation.strategy.invoke(acc, params)
            } ?: acc
        }
    }

    private suspend fun processAppendQueue() {

        println("ENTERED WHILE LOOP")

        _processingAppendQueue.update { true }

        var keepFetching = true

        while (appendLoadParamsQueue.isNotEmpty() && keepFetching) {


            if (pendingSkipQueueJobs.isNotEmpty()) {
                println("PENDING SKIP QUEUE JOBS = $pendingSkipQueueJobs")
                // TODO(): Design decision to let existing process queue job complete but wait to continue until the skip queue request(s) is completed
                // Need to wait until the skip queue request is completed
                delay(100)
                continue
            }


            val pagingState = _mutablePagingState.value
            val fetchingState = fetchingStateHolder.state.value

            val firstQueueElement = appendLoadParamsQueue.first()


            // TODO(): Design decision
            // If enqueued by the user, then we want to update max request and fetch
            // We want to track the load positions separate from access positions


            if (firstQueueElement.mechanism == LoadParamsQueueElement.Mechanism.EnqueueRequest || fetchingStrategy.shouldFetchForward(
                    firstQueueElement.params,
                    pagingState,
                    fetchingState
                )
            ) {
                println("SHOULD FETCH")

                val queueElement = appendLoadParamsQueue.removeFirst()


                // TODO(): Do we need to use JobCoordinator?

                // Apply middleware only once, if any
                handleAppendLoading(queueElement.params.applyMiddleware())

            } else {
                println("SHOULD NOT FETCH")
                keepFetching = false
            }

        }

        _processingAppendQueue.update { false }
        println("EXITED WHILE LOOP")
    }

    private suspend fun handlePrependLoading(
        loadParams: PagingSource.LoadParams<K>,
        retryCount: Int = 0
    ) {

        updateStateWithPrependLoading()

        try {

            println("*** HITTING IN PREPEND LOADING")
            normalizedStore.loadPage(loadParams).first {

                println("*** PREPEND LOADING RESULT = $it")

                when (it) {
                    is PageLoadStatus.Empty -> {
                        // TODO(): Enable debug logging
                        true
                    }

                    is PageLoadStatus.Error -> {
                        val encodedError =
                            Json.encodeToString(registry.error.serializer(), it.error)
                        throw PagingError(encodedError, it.extras)
                    }

                    is PageLoadStatus.Loading -> {
                        updateStateWithPrependLoading() // TODO(): Is this right?
                        false
                    }

                    is PageLoadStatus.Processing -> {
                        // TODO(): Enable debug logging
                        false
                    }

                    is PageLoadStatus.SkippingLoad -> {
                        true
                    }

                    is PageLoadStatus.Success -> {
                        // Update state
                        updateStateWithPrependData(it, loadParams.key)

                        // Clear prepend queue
                        prependLoadParamsQueue.clear()

                        it.prevKey?.let { key ->

                            val loadParams = PagingSource.LoadParams(
                                key,
                                LoadStrategy.SkipCache,
                                LoadDirection.Prepend
                            )



                            prependLoadParamsQueue.addLast(
                                LoadParamsQueueElement(
                                    loadParams, mechanism = LoadParamsQueueElement.Mechanism.NetworkLoadResponse
                                )
                            )
                        }

                        updateExistingPendingQueueJob(loadParams.key, inFlight = false, completed = true)

                        true
                    }
                }
            }

        } catch (pagingError: PagingError) {

            when (errorHandlingStrategy) {
                ErrorHandlingStrategy.Ignore -> {
                    // Ignore

                    // Clear prepend queue
                    prependLoadParamsQueue.clear()
                }

                ErrorHandlingStrategy.PassThrough -> {
                    // No placeholders to remove

                    val error =
                        Json.decodeFromString(registry.error.serializer(), pagingError.encodedError)

                    updateStateWithPrependError(error, pagingError.extras)

                    // Clear prepend queue
                    prependLoadParamsQueue.clear()

                }

                is ErrorHandlingStrategy.RetryLast -> {
                    if (retryCount < errorHandlingStrategy.maxRetries) {
                        handlePrependLoading(loadParams, retryCount + 1)
                    } else {
                        val error = Json.decodeFromString(
                            registry.error.serializer(),
                            pagingError.encodedError
                        )

                        updateStateWithPrependError(error, pagingError.extras)
                    }
                }
            }

        }
    }

    private fun addPendingQueueJob(key: K) {
        pendingSkipQueueJobs[key] = PendingSkipQueueJob(key, false)
    }


    private fun updateExistingPendingQueueJob(key: K, inFlight: Boolean, completed: Boolean) {

        if (key !in pendingSkipQueueJobs) return

        if (completed) {
            pendingSkipQueueJobs.remove(key)
        } else {
            pendingSkipQueueJobs[key] = PendingSkipQueueJob(key, inFlight)
        }
    }

    private suspend fun handleAppendLoading(
        loadParams: PagingSource.LoadParams<K>,
        retryCount: Int = 0,
        addNextToQueue: Boolean = true
    ) {

        println("HANDLE APPEND LOADING!!!! ${loadParams.key}")
        updateStateWithAppendLoading()

        try {

            fetchingStateHolder.updateMaxRequestSoFar(loadParams.key)

            normalizedStore.loadPage(loadParams).first {
                println("FIRST for ${loadParams.key} - $it")
                when (it) {
                    is PageLoadStatus.Empty -> {
                        // TODO(): Enable debug logging

                        updateExistingPendingQueueJob(loadParams.key, inFlight = false, completed = true)

                        true

                    }

                    is PageLoadStatus.Error -> {
                        val encodedError =
                            Json.encodeToString(registry.error.serializer(), it.error)
                        throw PagingError(encodedError, it.extras)
                    }

                    is PageLoadStatus.Loading -> {

                        updateExistingPendingQueueJob(
                            loadParams.key,
                            inFlight = true,
                            completed = false
                        )


                        println("HITTING IN UPDATE WITH LOADING")

                        _mutablePagingState.value = PagingState(
                            ids = _mutablePagingState.value.ids,
                            loadStates = _mutablePagingState.value.loadStates.copy(
                                append = PagingLoadState.Loading(_mutablePagingState.value.loadStates.append.extras)
                            )
                        )
                        println("UPDATED LOADING STATE")

                        false
                    }

                    is PageLoadStatus.Processing -> {
                        // TODO(): Enable debug logging
                        false
                    }

                    is PageLoadStatus.SkippingLoad -> {
                        updateExistingPendingQueueJob(loadParams.key, inFlight = false, completed = true)

                        true
                    }

                    is PageLoadStatus.Success -> {
                        // Update state
                        println("HITTING IN UPDATE WITH SUCCESS")
                        updateStateWithAppendData(it, loadParams.key)

                        // Load next key, if not null
                        // TODO(): Design decision to skip cache on incremental loads
                        if (addNextToQueue) {
                            it.nextKey?.let { key ->
                                appendLoadParamsQueue.addLast(
                                    LoadParamsQueueElement(
                                        PagingSource.LoadParams(
                                            key,
                                            LoadStrategy.SkipCache,
                                            LoadDirection.Append
                                        ),
                                        mechanism = LoadParamsQueueElement.Mechanism.NetworkLoadResponse
                                    )
                                )
                            }

                        }

                        updateExistingPendingQueueJob(loadParams.key, inFlight = false, completed = true)


                        true
                    }
                }
            }

        } catch (pagingError: PagingError) {
            println("HITTING IN HANDLE APPEND ERROR")
            handleAppendPagingError(pagingError, loadParams, retryCount, addNextToQueue)
        }

        println("FINISHED!!!! ${loadParams.key}")
    }

    private suspend fun handleAppendPagingError(
        pagingError: PagingError,
        loadParams: PagingSource.LoadParams<K>,
        retryCount: Int,
        addNextToQueue: Boolean
    ) {
        when (errorHandlingStrategy) {
            ErrorHandlingStrategy.Ignore -> {
                // Ignore

                updateExistingPendingQueueJob(loadParams.key, inFlight = true, completed = true)

            }

            ErrorHandlingStrategy.PassThrough -> {
                // TODO(): Design decision to remove placeholders when passing an error through
                normalizedStore.clear(loadParams.key)

                val error =
                    Json.decodeFromString(registry.error.serializer(), pagingError.encodedError)

                updateExistingPendingQueueJob(loadParams.key, inFlight = true, completed = true)

                updateStateWithAppendError(error, pagingError.extras)
            }

            is ErrorHandlingStrategy.RetryLast -> {

                if (retryCount < errorHandlingStrategy.maxRetries) {
                    handleAppendLoading(loadParams, retryCount + 1, addNextToQueue = addNextToQueue)
                } else {

                    val error =
                        Json.decodeFromString(
                            registry.error.serializer(),
                            pagingError.encodedError
                        )

                    updateExistingPendingQueueJob(loadParams.key, inFlight = true, completed = true)

                    updateStateWithAppendError(error, pagingError.extras)
                }

            }
        }

    }

    private fun handleLaunchEffects() {
        coroutineScope.launch {
            launchEffects.forEach { launchEffect -> launchEffect.invoke() }
            println("COMPLETED COROUTINE FOR LAUNCH EFFECTS")
        }
    }

    // TODO(): Design decision to have eager loading done in the background, when we don't have composition
    // TODO(): Design decision to separate fetching state from eager loading
    private fun handleEagerLoading() {
        coroutineScope.launch {
            appendLoadParamsQueue.addLast(
                LoadParamsQueueElement(
                    initialLoadParams,
                    LoadParamsQueueElement.Mechanism.InitialLoad
                )
            )
            processAppendQueue()
            println("COMPLETED COROUTINE FOR EAGER LOADING")
        }
    }


    private fun updateStateWithAppendData(data: PageLoadStatus.Success<Id, Q, K, V, E>, key: K?) {

        val transformedSnapshot = transformSnapshot(data.snapshot, key)

        _mutablePagingState.value = PagingState<Id, Q, E>(
            ids = transformedSnapshot.getAllIds(),
            loadStates = _mutablePagingState.value.loadStates.copy(
                append = PagingLoadState.NotLoading(data.nextKey == null)
            )
        )

        println("UPDATED APPEND DATA STATE ${_mutablePagingState.value.ids.size}")

    }

    private fun updateStateWithPrependData(data: PageLoadStatus.Success<Id, Q, K, V, E>, key: K?) {
        val prevState = _mutablePagingState.value

        println("PREV STATE = $prevState")
        val transformedSnapshot = transformSnapshot(data.snapshot, key)
        println("TRANSFORMED SNAPSHOT = $transformedSnapshot")

        _mutablePagingState.value = PagingState(
            ids = transformedSnapshot.getAllIds(),
            loadStates = prevState.loadStates.copy(
                prepend = PagingLoadState.NotLoading(data.prevKey == null)
            )
        )

    }

    private fun updateStateWithAppendError(error: E, extras: JsonObject?) {
        val prevState = _mutablePagingState.value
        _mutablePagingState.value = PagingState(
            ids = prevState.ids,
            loadStates = prevState.loadStates.copy(
                append = PagingLoadState.Error(error, extras)
            )
        )
    }

    private fun updateStateWithPrependError(error: E, extras: JsonObject?) {
        val prevState = _mutablePagingState.value
        _mutablePagingState.value = PagingState(
            ids = prevState.ids,
            loadStates = prevState.loadStates.copy(
                prepend = PagingLoadState.Error(error, extras)
            )
        )
    }


    private fun updateStateWithPrependLoading() {
        val prevState = _mutablePagingState.value
        _mutablePagingState.value = PagingState(
            ids = prevState.ids,
            loadStates = prevState.loadStates.copy(
                prepend = PagingLoadState.Loading(prevState.loadStates.prepend.extras)
            )
        )
    }

    private fun updateStateWithAppendLoading() {

        _mutablePagingState.update {
            PagingState(
                ids = it.ids,
                loadStates = it.loadStates.copy(
                    append = PagingLoadState.Loading(it.loadStates.append.extras)
                )
            )
        }

        println("UPDATED LOADING STATE ${_mutablePagingState.value.ids.size}")
    }


    private suspend fun PagingSource.LoadParams<K>.applyMiddleware(
        index: Int = 0
    ): PagingSource.LoadParams<K> {
        return if (index < middleware.size) {
            middleware[index].apply(this) { nextParams ->
                nextParams.applyMiddleware(index + 1)
            }
        } else {
            this
        }
    }


}