package org.mobilenativefoundation.storex.paging.internal.impl

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.serializer
import org.mobilenativefoundation.storex.paging.Identifiable
import org.mobilenativefoundation.storex.paging.ItemSnapshotList
import org.mobilenativefoundation.storex.paging.Pager
import org.mobilenativefoundation.storex.paging.PagingLoadState
import org.mobilenativefoundation.storex.paging.PagingSource
import org.mobilenativefoundation.storex.paging.PagingState
import org.mobilenativefoundation.storex.paging.Quantifiable
import org.mobilenativefoundation.storex.paging.SelfUpdatingItem
import org.mobilenativefoundation.storex.paging.custom.ErrorHandlingStrategy
import org.mobilenativefoundation.storex.paging.custom.FetchingStrategy
import org.mobilenativefoundation.storex.paging.custom.LaunchEffect
import org.mobilenativefoundation.storex.paging.custom.Middleware
import org.mobilenativefoundation.storex.paging.custom.Operation
import org.mobilenativefoundation.storex.paging.internal.api.FetchingStateHolder

// TODO(): Design decision to support initial state (e.g., hardcoded)

@OptIn(InternalSerializationApi::class)
class RealPager<Id : Comparable<Id>, K : Any, V : Identifiable<Id>, E : Any, P : Any>(
    coroutineDispatcher: CoroutineDispatcher,
    private val fetchingStateHolder: FetchingStateHolder<Id>,
    private val launchEffects: List<LaunchEffect>,
    private val errorHandlingStrategy: ErrorHandlingStrategy,
    private val middleware: List<Middleware<K>>,
    private val fetchingStrategy: FetchingStrategy<Id, K, E>,
    private val initialLoadParams: PagingSource.LoadParams<K>,
    private val registry: KClassRegistry<Id, K, V, E>,
    private val normalizedStore: RealNormalizedStore<Id, K, V, E>,
    private val operations: List<Operation<Id, K, V, P, P>>,
    private val initialState: PagingState<Id, E> = PagingState.initial()
) : Pager<Id, K, V, E> {

    private val coroutineScope = CoroutineScope(coroutineDispatcher)

    // TODO(): This is not thread safe!
    private val _mutablePagingState = MutableStateFlow(initialState)
    override val pagingState: StateFlow<PagingState<Id, E>> = _mutablePagingState.asStateFlow()

    private val appendLoadParamsQueue: LoadParamsQueue<K> = LoadParamsQueue()
    private val prependLoadParamsQueue: LoadParamsQueue<K> = LoadParamsQueue()


    init {
        handleLaunchEffects()
        handleEagerLoading()
    }

    override fun selfUpdatingItem(id: Quantifiable<Id>): SelfUpdatingItem<Id, V, E> {
        return normalizedStore.selfUpdatingItem(id)
    }

    override fun invoke(loadParams: Flow<PagingSource.LoadParams<K>>) {
        coroutineScope.launch {
            loadParams.collect {
                println(it)
            }
        }
    }

//    @Composable
//    override fun pagingState(loadParams: Flow<PagingSource.LoadParams<K>>): PagingState<Id, E> {
//        val composableCoroutineScope = rememberCoroutineScope { DispatcherProvider.io }
//
//        val params by loadParams.collectAsState(initialLoadParams)
//
//        val fetchingState by fetchingStateHolder.state.collectAsState()
//
//        val pagingState by _pagingState.collectAsState()
//
//        println("TAG = ${pagingState.loadStates.append}")
//        println("TAG= ${pagingState.ids}")
//
////        LaunchedEffect(params) {
////
////            println("LAUNCHING 1")
////            when (params.direction) {
////                PagingSource.LoadParams.Direction.Prepend -> {
////                    prependLoadParamsQueue.addLast(params)
////                    processPrependQueue()
////                }
////
////                PagingSource.LoadParams.Direction.Append -> {
////                    appendLoadParamsQueue.addLast(params)
////                    processAppendQueue()
////                }
////            }
////        }
//
////        LaunchedEffect(fetchingState) {
////            println("LAUNCHING 2")
////
////            handleForwardPrefetching()
////            handleBackwardPrefetching()
////        }
//
//
//        return pagingState
//    }

    // TODO(): Design decision to get the most recent prepend load params
    private fun processPrependQueue() {

        while (prependLoadParamsQueue.isNotEmpty()) {
            val latestPrependLoadParams = prependLoadParamsQueue.removeLast()

            // TODO(): Design decision not to check whether to fetch for prepend loads

            // TODO(): Design decision not to prepend placeholders

            // Apply middleware only once, if any
            coroutineScope.launch {
                handlePrependLoading(latestPrependLoadParams.applyMiddleware())
            }
        }
    }


    private fun transformSnapshot(
        snapshot: ItemSnapshotList<Id, V>,
        key: K?,
    ): ItemSnapshotList<Id, V> {

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

        var keepFetching = true

        while (appendLoadParamsQueue.isNotEmpty() && keepFetching) {
            val pagingState = _mutablePagingState.value
            val fetchingState = fetchingStateHolder.state.value

            if (fetchingStrategy.shouldFetchForward(
                    appendLoadParamsQueue.first(),
                    pagingState,
                    fetchingState
                )
            ) {
                println("SHOULD FETCH")

                val loadParams = appendLoadParamsQueue.removeFirst()


                // TODO(): Do we need to use JobCoordinator?

                // Apply middleware only once, if any
                handleAppendLoading(loadParams.applyMiddleware())

            } else {
                println("SHOULD NOT FETCH")
                keepFetching = false
            }

        }
    }

    private suspend fun handlePrependLoading(
        loadParams: PagingSource.LoadParams<K>,
        retryCount: Int = 0
    ) {

        updateStateWithPrependLoading()

        try {
            normalizedStore.loadPage(loadParams).first {
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
                        updateStateWithAppendLoading() // TODO(): Is this right?
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
                            prependLoadParamsQueue.addLast(
                                PagingSource.LoadParams(
                                    key,
                                    PagingSource.LoadParams.Strategy.SkipCache,
                                    PagingSource.LoadParams.Direction.Prepend
                                )
                            )
                        }

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

    private suspend fun handleAppendLoading(
        loadParams: PagingSource.LoadParams<K>,
        retryCount: Int = 0
    ) {

        updateStateWithAppendLoading()

        try {
            normalizedStore.loadPage(loadParams).first {
                println("FIRST $it")
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

                        println("HITTING IN UPDATE WITH LOADING")

                        suspend {
                            _mutablePagingState.value = PagingState(
                                ids = _mutablePagingState.value.ids,
                                loadStates = _mutablePagingState.value.loadStates.copy(
                                    append = PagingLoadState.Loading(_mutablePagingState.value.loadStates.append.extras)
                                )
                            )
                            println("UPDATED LOADING STATE")

                        }

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
                        println("HITTING IN UPDATE WITH SUCCESS")
                        updateStateWithAppendData(it, loadParams.key)

                        // Load next key, if not null
                        // TODO(): Design decision to skip cache on incremental loads
                        it.nextKey?.let { key ->
                            appendLoadParamsQueue.addLast(
                                PagingSource.LoadParams(
                                    key,
                                    PagingSource.LoadParams.Strategy.SkipCache,
                                    PagingSource.LoadParams.Direction.Append
                                )
                            )
                        }

                        true
                    }
                }
            }

        } catch (pagingError: PagingError) {
            handleAppendPagingError(pagingError, loadParams, retryCount)
        }
    }

    private suspend fun handleAppendPagingError(
        pagingError: PagingError,
        loadParams: PagingSource.LoadParams<K>,
        retryCount: Int
    ) {
        when (errorHandlingStrategy) {
            ErrorHandlingStrategy.Ignore -> {
                // Ignore
            }

            ErrorHandlingStrategy.PassThrough -> {
                // TODO(): Design decision to remove placeholders when passing an error through
                normalizedStore.clear(loadParams.key)

                val error =
                    Json.decodeFromString(registry.error.serializer(), pagingError.encodedError)

                updateStateWithAppendError(error, pagingError.extras)
            }

            is ErrorHandlingStrategy.RetryLast -> {

                if (retryCount < errorHandlingStrategy.maxRetries) {
                    handleAppendLoading(loadParams, retryCount + 1)
                } else {

                    val error =
                        Json.decodeFromString(
                            registry.error.serializer(),
                            pagingError.encodedError
                        )

                    updateStateWithAppendError(error, pagingError.extras)
                }

            }
        }

    }

    private fun handleLaunchEffects() {
        coroutineScope.launch {
            launchEffects.forEach { launchEffect -> launchEffect.invoke() }
        }
    }

    // TODO(): Design decision to have eager loading done in the background, when we don't have composition
    // TODO(): Design decision to separate fetching state from eager loading
    private fun handleEagerLoading() {
        coroutineScope.launch {
            appendLoadParamsQueue.addLast(initialLoadParams)
            processAppendQueue()
        }
    }


    private fun updateStateWithAppendData(data: PageLoadStatus.Success<Id, K, V, E>, key: K?) {

        val transformedSnapshot = transformSnapshot(data.snapshot, key)

        _mutablePagingState.value = PagingState(
            ids = transformedSnapshot.getAllIds(),
            loadStates = _mutablePagingState.value.loadStates.copy(
                append = PagingLoadState.NotLoading(data.nextKey == null)
            )
        )

        println(_mutablePagingState.value.ids.toString())

    }

    private fun updateStateWithPrependData(data: PageLoadStatus.Success<Id, K, V, E>, key: K?) {
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

        println("UPDATED LOADING STATE")
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