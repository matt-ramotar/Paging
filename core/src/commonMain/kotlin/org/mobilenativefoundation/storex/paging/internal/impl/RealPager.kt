package org.mobilenativefoundation.storex.paging.internal.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.serializer
import org.mobilenativefoundation.store5.core.Identifiable
import org.mobilenativefoundation.storex.paging.ItemSnapshotList
import org.mobilenativefoundation.storex.paging.LoadPageStatus
import org.mobilenativefoundation.storex.paging.Pager
import org.mobilenativefoundation.storex.paging.PagingConfig
import org.mobilenativefoundation.storex.paging.PagingLoadState
import org.mobilenativefoundation.storex.paging.PagingSource
import org.mobilenativefoundation.storex.paging.PagingState
import org.mobilenativefoundation.storex.paging.RealNormalizedStore
import org.mobilenativefoundation.storex.paging.SelfUpdatingItem
import org.mobilenativefoundation.storex.paging.custom.ErrorFactory
import org.mobilenativefoundation.storex.paging.custom.ErrorHandlingStrategy
import org.mobilenativefoundation.storex.paging.custom.FetchingStrategy
import org.mobilenativefoundation.storex.paging.custom.LaunchEffect
import org.mobilenativefoundation.storex.paging.custom.Middleware
import org.mobilenativefoundation.storex.paging.custom.SideEffect
import org.mobilenativefoundation.storex.paging.custom.TransformationStrategy
import org.mobilenativefoundation.storex.paging.internal.api.FetchingStateHolder

// TODO(): Design decision to support initial state (e.g., hardcoded)

@OptIn(InternalSerializationApi::class)
class RealPager<Id : Comparable<Id>, K : Any, V : Identifiable<Id>, E : Any, P : Any>(
    coroutineDispatcher: CoroutineDispatcher,
    private val fetchingStateHolder: FetchingStateHolder<Id>,
    private val launchEffects: List<LaunchEffect>,
    private val sideEffects: List<SideEffect<Id, V>>,
    private val errorHandlingStrategy: ErrorHandlingStrategy,
    private val middleware: List<Middleware<K>>,
    private val fetchingStrategy: FetchingStrategy<Id, K, E>,
    private val pagingConfig: PagingConfig<Id>,
    private val initialLoadParams: PagingSource.LoadParams<K>,
    private val errorFactory: ErrorFactory<E>,
    private val registry: KClassRegistry<Id, K, V, E>,
    private val normalizedStore: RealNormalizedStore<Id, K, V, E>,
    private val transformationParams: P,
    private val transformations: List<TransformationStrategy<Id, V, P>>,
    initialState: PagingState<Id, E> = PagingState.initial()
) : Pager<Id, K, V, E> {

    private val coroutineScope = CoroutineScope(coroutineDispatcher)

    // TODO(): This is not thread safe!
    private val _mutablePagingState = MutableStateFlow(initialState)

    private val appendLoadParamsQueue: LoadParamsQueue<K> = LoadParamsQueue()
    private val prependLoadParamsQueue: LoadParamsQueue<K> = LoadParamsQueue()


    init {
        handleLaunchEffects()
        handleEagerLoading()
    }

    override fun selfUpdatingItem(id: Id): SelfUpdatingItem<Id, V, E> {
        return normalizedStore.selfUpdatingItem(id)
    }

    @Composable
    override fun pagingState(loadParams: StateFlow<PagingSource.LoadParams<K>>): PagingState<Id, E> {

        val params by loadParams.collectAsState()


        val fetchingState by fetchingStateHolder.state.collectAsState()

        val pagingState by _mutablePagingState.collectAsState()

        LaunchedEffect(params) {
            when (params.direction) {
                PagingSource.LoadParams.Direction.Prepend -> {
                    prependLoadParamsQueue.addLast(params)
                    processPrependQueue()
                }

                PagingSource.LoadParams.Direction.Append -> {
                    appendLoadParamsQueue.addLast(params)
                    processAppendQueue()
                }
            }
        }

        LaunchedEffect(fetchingState) {
            if (fetchingStrategy.shouldFetchForward(params, pagingState, fetchingState)) {
                handleForwardPrefetching()
            }

            if (fetchingStrategy.shouldFetchBackward(params, pagingState, fetchingState)) {
                handleBackwardPrefetching()
            }
        }

        return pagingState
    }

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

    private fun handleForwardPrefetching() {
        processAppendQueue()
    }

    private fun handleBackwardPrefetching() {
        processPrependQueue()
    }

    private fun transformSnapshot(snapshot: ItemSnapshotList<Id, V>): ItemSnapshotList<Id, V> {
        var transformed = snapshot

        transformations.forEach { transformation ->
            transformed = transformation(transformed, transformationParams)
        }

        return transformed
    }

    private fun processAppendQueue() {

        while (appendLoadParamsQueue.isNotEmpty()) {
            val pagingState = _mutablePagingState.value
            val fetchingState = fetchingStateHolder.state.value

            if (fetchingStrategy.shouldFetchForward(
                    appendLoadParamsQueue.first(),
                    pagingState,
                    fetchingState
                )
            ) {
                val loadParams = appendLoadParamsQueue.removeFirst()


                // TODO(): Do we need to use JobCoordinator?

                // Apply middleware only once, if any
                coroutineScope.launch {
                    handleAppendLoading(loadParams.applyMiddleware())
                }
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
                    is LoadPageStatus.Empty -> {
                        // TODO(): Enable debug logging
                        true
                    }

                    is LoadPageStatus.Error -> {
                        val encodedError =
                            Json.encodeToString(registry.error.serializer(), it.error)
                        throw PagingError(encodedError, it.extras)
                    }

                    is LoadPageStatus.Loading -> {
                        updateStateWithAppendLoading()
                        false
                    }

                    is LoadPageStatus.Processing -> {
                        // TODO(): Enable debug logging
                        false
                    }

                    is LoadPageStatus.SkippingLoad -> {
                        true
                    }

                    is LoadPageStatus.Success -> {
                        // Update state
                        updateStateWithPrependData(it)

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
                when (it) {
                    is LoadPageStatus.Empty -> {
                        // TODO(): Enable debug logging
                        true
                    }

                    is LoadPageStatus.Error -> {
                        val encodedError =
                            Json.encodeToString(registry.error.serializer(), it.error)
                        throw PagingError(encodedError, it.extras)
                    }

                    is LoadPageStatus.Loading -> {
                        updateStateWithAppendLoading()
                        false
                    }

                    is LoadPageStatus.Processing -> {
                        // TODO(): Enable debug logging
                        false
                    }

                    is LoadPageStatus.SkippingLoad -> {
                        true
                    }

                    is LoadPageStatus.Success -> {
                        // Update state
                        updateStateWithAppendData(it)

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


    private fun updateStateWithAppendData(data: LoadPageStatus.Success<Id, K, V, E>) {
        val prevState = _mutablePagingState.value

        val transformedSnapshot = transformSnapshot(data.snapshot)

        _mutablePagingState.value = PagingState(
            ids = transformedSnapshot.getAllIds(),
            loadStates = prevState.loadStates.copy(
                append = PagingLoadState.NotLoading(data.nextKey == null)
            )
        )

    }

    private fun updateStateWithPrependData(data: LoadPageStatus.Success<Id, K, V, E>) {
        val prevState = _mutablePagingState.value

        val transformedSnapshot = transformSnapshot(data.snapshot)

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
        val prevState = _mutablePagingState.value
        _mutablePagingState.value = PagingState(
            ids = prevState.ids,
            loadStates = prevState.loadStates.copy(
                append = PagingLoadState.Loading(prevState.loadStates.append.extras)
            )
        )
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