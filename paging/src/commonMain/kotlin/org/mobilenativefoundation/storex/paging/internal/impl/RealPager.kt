package org.mobilenativefoundation.storex.paging.internal.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import app.cash.molecule.launchMolecule
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.JsonObject
import org.mobilenativefoundation.storex.paging.*
import org.mobilenativefoundation.storex.paging.custom.*
import org.mobilenativefoundation.storex.paging.internal.api.FetchingStateHolder
import org.mobilenativefoundation.storex.paging.internal.api.NormalizedStore


// TODO(): Design decision to support initial state (e.g., hardcoded)


@OptIn(InternalSerializationApi::class)
class RealPager<Id : Identifier<Id>, K : Comparable<K>, V : Identifiable<Id>>(
    coroutineDispatcher: CoroutineDispatcher,
    private val fetchingStateHolder: FetchingStateHolder<Id, K>,
    private val launchEffects: List<LaunchEffect>,
    private val errorHandlingStrategy: ErrorHandlingStrategy,
    private val middleware: List<Middleware<K>>,
    private val fetchingStrategy: FetchingStrategy<Id, K>,
    private val initialLoadParams: PagingSource.LoadParams<K>,
    private val registry: KClassRegistry<Id, K, V>,
    private val concurrentNormalizedStore: NormalizedStore<Id, K, V>,
    initialState: PagingState<Id> = PagingState.initial(),
) : Pager<Id, K, V> {

    private val coroutineScope = CoroutineScope(coroutineDispatcher)

    // TODO(): This is not thread safe!
    private val _mutablePagingState = MutableStateFlow(initialState)
    private val _processingAppendQueue = MutableStateFlow(false)

    private val pendingSkipQueueJobs = mutableMapOf<K, PendingSkipQueueJob<K>>()


    private val appendLoadParamsQueue: LoadParamsQueue<K> = LoadParamsQueue()
    private val prependLoadParamsQueue: LoadParamsQueue<K> = LoadParamsQueue()


    init {
        handleLaunchEffects()
        handleEagerLoading()
    }

    private val operations = mutableListOf<Operation<Id, K, V>>()
    private var lastUntransformedSnapshot: ItemSnapshotList<Id, V>? = null

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
    ): Flow<PagingState<Id>> =
        coroutineScope.launchMolecule(recompositionMode.toCashRecompositionMode()) {
            pagingState(requests)
        }

    override fun pagingStateFlow(
        coroutineScope: CoroutineScope,
        requests: Flow<PagingRequest<K>>
    ): StateFlow<PagingState<Id>> {

        println("*** BEFORE LAUNCHING MOLECULE")
        return coroutineScope.launchMolecule(RecompositionMode.ContextClock.toCashRecompositionMode()) {
            pagingState(requests)
        }
    }

    // TODO(): This is not efficient - we should extract the common logic from [pagingState] - we shouldn't be getting ids and then remapping to values when we have values initially
    override fun pagingItems(coroutineScope: CoroutineScope, requests: Flow<PagingRequest<K>>): StateFlow<List<V>> {
        return coroutineScope.launchMolecule(RecompositionMode.ContextClock.toCashRecompositionMode()) {
            pagingState(requests).ids.mapNotNull { id -> id?.let { concurrentNormalizedStore.getItem(it) } }
        }
    }

    private suspend fun handleForwardPrefetching() {
        processAppendQueue()
    }

    private fun handleBackwardPrefetching() {
        processPrependQueue()
    }


    @Composable
    private fun pagingState(requests: Flow<PagingRequest<K>>): PagingState<Id> {
        val fetchingState by fetchingStateHolder.state.collectAsState()

        val pagingState by _mutablePagingState.collectAsState()

        println("TAG = ${pagingState.loadStates.append}")
        println("TAG= ${pagingState.ids}")

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
                                val queueElement = LoadParamsQueue.Element(
                                    params = request.toPagingSourceLoadParams(),
                                    mechanism = LoadParamsQueue.Element.Mechanism.EnqueueRequest
                                )
                                prependLoadParamsQueue.addLast(queueElement)
                                processPrependQueue()
                            }

                            LoadDirection.Append -> {
                                val queueElement = LoadParamsQueue.Element(
                                    params = request.toPagingSourceLoadParams(),
                                    mechanism = LoadParamsQueue.Element.Mechanism.EnqueueRequest
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

                        concurrentNormalizedStore.invalidate()
                    }
                }
            }
            println("END OF LAUNCHED EFFECT")
        }

        LaunchedEffect(fetchingState) {
            println("LAUNCHING 2")

            handleForwardPrefetching()
            handleBackwardPrefetching()
        }


        LaunchedEffect(fetchingState.minItemAccessedSoFar) {
            println("&&&& - FETCHING STATE MIN ITEM ACCESSED CHANGED ${fetchingState.minItemAccessedSoFar}")
            processPrependQueue()
        }

        LaunchedEffect(fetchingState.maxItemAccessedSoFar) {
            println("&&&& - FETCHING STATE MAX ITEM ACCESSED CHANGED ${fetchingState.maxItemAccessedSoFar}")
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


//    private fun applyOperations(
//        snapshot: ItemSnapshotList<Id, Q, V>,
//        key: K?,
//    ): ItemSnapshotList<Id, Q, V> {
//
//        if (operations.isEmpty()) {
//            return snapshot
//        }
//
//        val pagingState = _mutablePagingState.value
//        val fetchingState = fetchingStateHolder.state.value
//
//        return operations.fold(snapshot) { acc, operation ->
//            operation.shouldApply(key, pagingState, fetchingState)?.let { params ->
//                operation.strategy.invoke(acc, params)
//            } ?: acc
//        }
//    }

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


            if (firstQueueElement.mechanism == LoadParamsQueue.Element.Mechanism.EnqueueRequest || fetchingStrategy.shouldFetchForward(
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
            concurrentNormalizedStore.loadPage(loadParams).first {

                println("*** PREPEND LOADING RESULT = $it")

                when (it) {
                    is PageLoadState.Empty -> {
                        // TODO(): Enable debug logging
                        true
                    }

                    is PageLoadState.Error.Exception -> {
                        val errorMessage = it.error.message.orEmpty()
                        TODO()
                    }

                    is PageLoadState.Error.Message -> {
                        TODO()
                    }

                    is PageLoadState.Loading -> {
                        updateStateWithPrependLoading() // TODO(): Is this right?
                        false
                    }

                    is PageLoadState.Processing -> {
                        // TODO(): Enable debug logging
                        false
                    }

                    is PageLoadState.SkippingLoad -> {
                        true
                    }

                    is PageLoadState.Success -> {
                        // Update state
                        updateStateWithPrependData(it, loadParams.key)

                        // Clear prepend queue
                        prependLoadParamsQueue.clear()

                        it.prevKey?.let { key ->

                            val nextLoadParams = PagingSource.LoadParams(
                                key,
                                LoadStrategy.SkipCache,
                                LoadDirection.Prepend
                            )



                            prependLoadParamsQueue.addLast(
                                LoadParamsQueue.Element(
                                    nextLoadParams, mechanism = LoadParamsQueue.Element.Mechanism.NetworkLoadResponse
                                )
                            )
                        }

                        updateExistingPendingQueueJob(loadParams.key, inFlight = false, completed = true)

                        true
                    }
                }
            }

        } catch (pagingError: Throwable) {

            when (errorHandlingStrategy) {
                ErrorHandlingStrategy.Ignore -> {
                    // Ignore

                    // Clear prepend queue
                    prependLoadParamsQueue.clear()
                }

                ErrorHandlingStrategy.PassThrough -> {
                    // No placeholders to remove


                    updateStateWithPrependError(pagingError, null)

                    // Clear prepend queue
                    prependLoadParamsQueue.clear()

                }

                is ErrorHandlingStrategy.RetryLast -> {
                    if (retryCount < errorHandlingStrategy.maxRetries) {
                        handlePrependLoading(loadParams, retryCount + 1)
                    } else {

                        updateStateWithPrependError(pagingError, null)
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

            concurrentNormalizedStore.loadPage(loadParams).first {
                println("FIRST for ${loadParams.key} - $it")
                when (it) {
                    is PageLoadState.Empty -> {
                        // TODO(): Enable debug logging

                        updateExistingPendingQueueJob(loadParams.key, inFlight = false, completed = true)

                        true

                    }

                    is PageLoadState.Error -> {
//                        val encodedError =
//                            Json.encodeToString(registry.error.serializer(), it.error)
//                        throw PagingError(encodedError, it.extras)
                        // TODO(): Revisit extras
                        throw Throwable()
                    }

                    is PageLoadState.Loading -> {

                        updateExistingPendingQueueJob(
                            loadParams.key,
                            inFlight = true,
                            completed = false
                        )


                        println("HITTING IN UPDATE WITH LOADING")

                        _mutablePagingState.value = PagingState(
                            ids = _mutablePagingState.value.ids,
                            loadStates = _mutablePagingState.value.loadStates.copy(
                                append = PagingLoadState.Loading
                            )
                        )
                        println("UPDATED LOADING STATE")

                        false
                    }

                    is PageLoadState.Processing -> {
                        // TODO(): Enable debug logging
                        false
                    }

                    is PageLoadState.SkippingLoad -> {
                        updateExistingPendingQueueJob(loadParams.key, inFlight = false, completed = true)

                        true
                    }

                    is PageLoadState.Success -> {
                        // Update state
                        println("HITTING IN UPDATE WITH SUCCESS")
                        updateStateWithAppendData(it, loadParams.key)

                        // Load next key, if not null
                        // TODO(): Design decision to skip cache on incremental loads
                        if (addNextToQueue) {
                            it.nextKey?.let { key ->
                                appendLoadParamsQueue.addLast(
                                    LoadParamsQueue.Element(
                                        PagingSource.LoadParams(
                                            key,
                                            LoadStrategy.SkipCache,
                                            LoadDirection.Append
                                        ),
                                        mechanism = LoadParamsQueue.Element.Mechanism.NetworkLoadResponse
                                    )
                                )
                            }

                        }

                        updateExistingPendingQueueJob(loadParams.key, inFlight = false, completed = true)


                        true
                    }
                }
            }

        } catch (pagingError: Throwable) {
            println("HITTING IN HANDLE APPEND ERROR")
            handleAppendPagingError(pagingError, loadParams, retryCount, addNextToQueue)
        }

        println("FINISHED!!!! ${loadParams.key}")
    }

    private suspend fun handleAppendPagingError(
        pagingError: Throwable,
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
                concurrentNormalizedStore.clear(loadParams.key)

//                val error =
//                    Json.decodeFromString(registry.error.serializer(), pagingError.encodedError)

                updateExistingPendingQueueJob(loadParams.key, inFlight = true, completed = true)

                updateStateWithAppendError(pagingError, null)
            }

            is ErrorHandlingStrategy.RetryLast -> {

                if (retryCount < errorHandlingStrategy.maxRetries) {
                    handleAppendLoading(loadParams, retryCount + 1, addNextToQueue = addNextToQueue)
                } else {

//                    val error =
//                        Json.decodeFromString(
//                            registry.error.serializer(),
//                            pagingError.encodedError
//                        )

                    updateExistingPendingQueueJob(loadParams.key, inFlight = true, completed = true)

                    updateStateWithAppendError(pagingError, null)
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
                LoadParamsQueue.Element(
                    initialLoadParams,
                    LoadParamsQueue.Element.Mechanism.InitialLoad
                )
            )
            processAppendQueue()
            println("COMPLETED COROUTINE FOR EAGER LOADING")
        }
    }


    private fun updateStateWithAppendData(data: PageLoadState.Success<Id, K, V>, key: K?) {

        lastUntransformedSnapshot = data.snapshot

        val transformedSnapshot = applyOperations(data.snapshot, key)

        _mutablePagingState.value = PagingState(
            ids = transformedSnapshot.getAllIds(),
            loadStates = _mutablePagingState.value.loadStates.copy(
                append = PagingLoadState.NotLoading(data.nextKey == null)
            )
        )

        println("UPDATED APPEND DATA STATE ${_mutablePagingState.value.ids.size}")

    }

    private fun updateStateWithPrependData(data: PageLoadState.Success<Id, K, V>, key: K?) {
        lastUntransformedSnapshot = data.snapshot

        val prevState = _mutablePagingState.value

        println("PREV STATE = $prevState")
        val transformedSnapshot = applyOperations(data.snapshot, key)
        println("TRANSFORMED SNAPSHOT = $transformedSnapshot")

        _mutablePagingState.value = PagingState(
            ids = transformedSnapshot.getAllIds(),
            loadStates = prevState.loadStates.copy(
                prepend = PagingLoadState.NotLoading(data.prevKey == null)
            )
        )

    }

    // TODO(): Revisit extras
    private fun updateStateWithAppendError(error: Throwable, extras: JsonObject?) {
        val prevState = _mutablePagingState.value
        _mutablePagingState.value = PagingState(
            ids = prevState.ids,
            loadStates = prevState.loadStates.copy(
                append = PagingLoadState.Error(error)
            )
        )
    }

    private fun updateStateWithPrependError(error: Throwable, extras: JsonObject?) {
        val prevState = _mutablePagingState.value

        _mutablePagingState.value = PagingState(
            ids = prevState.ids,
            loadStates = prevState.loadStates.copy(
                prepend = PagingLoadState.Error(error)
            )
        )
    }


    private fun updateStateWithPrependLoading() {
        val prevState = _mutablePagingState.value
        _mutablePagingState.value = PagingState(
            ids = prevState.ids,
            loadStates = prevState.loadStates.copy(
                prepend = PagingLoadState.Loading
            )
        )
    }

    private fun updateStateWithAppendLoading() {

        _mutablePagingState.update {
            PagingState(
                ids = it.ids,
                loadStates = it.loadStates.copy(
                    append = PagingLoadState.Loading
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

    private val operationCache =
        mutableMapOf<Pair<Operation<Id, K, V>, ItemSnapshotList<Id, V>>, ItemSnapshotList<Id, V>>()

    private fun applyOperations(snapshot: ItemSnapshotList<Id, V>, key: K?): ItemSnapshotList<Id, V> {
        if (operations.isEmpty()) {
            return snapshot
        }

        val pagingState = _mutablePagingState.value
        val fetchingState = fetchingStateHolder.state.value

        return operations.fold(snapshot) { acc, operation ->
            if (operation.shouldApply(key, pagingState, fetchingState)) {
                if ((operation to acc) in operationCache) {
                    operationCache[(operation to acc)]!!
                } else {
                    operation(acc)
                }
            } else {
                acc
            }
        }
    }

    private data class PendingSkipQueueJob<K : Any>(
        val key: K,
        val inFlight: Boolean,
    )
}