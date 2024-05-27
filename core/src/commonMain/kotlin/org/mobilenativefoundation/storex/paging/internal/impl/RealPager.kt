package org.mobilenativefoundation.storex.paging.internal.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.serializer
import org.mobilenativefoundation.store.store5.Store
import org.mobilenativefoundation.store.store5.impl.extensions.fresh
import org.mobilenativefoundation.store5.core.Identifiable
import org.mobilenativefoundation.storex.paging.ItemSnapshotList
import org.mobilenativefoundation.storex.paging.ItemState
import org.mobilenativefoundation.storex.paging.Pager
import org.mobilenativefoundation.storex.paging.PagingConfig
import org.mobilenativefoundation.storex.paging.PagingLoadState
import org.mobilenativefoundation.storex.paging.PagingSource
import org.mobilenativefoundation.storex.paging.PagingState
import org.mobilenativefoundation.storex.paging.SelfUpdatingItem
import org.mobilenativefoundation.storex.paging.custom.ErrorFactory
import org.mobilenativefoundation.storex.paging.custom.ErrorHandlingStrategy
import org.mobilenativefoundation.storex.paging.custom.FetchingStrategy
import org.mobilenativefoundation.storex.paging.custom.LaunchEffect
import org.mobilenativefoundation.storex.paging.custom.Middleware
import org.mobilenativefoundation.storex.paging.custom.SideEffect
import org.mobilenativefoundation.storex.paging.custom.TransformationStrategy
import org.mobilenativefoundation.storex.paging.internal.api.FetchingStateHolder
import org.mobilenativefoundation.storex.paging.internal.api.MutablePagingBuffer

// TODO(): Design decision to support initial state (e.g., hardcoded)

@OptIn(InternalSerializationApi::class)
class RealPager<Id : Comparable<Id>, K : Any, V : Identifiable<Id>, E : Any, P : Any>(
    coroutineDispatcher: CoroutineDispatcher,
    private val transformationParams: P,
    private val fetchingStateHolder: FetchingStateHolder<Id>,
    private val transformations: List<TransformationStrategy<Id, V, P>>,
    private val launchEffects: List<LaunchEffect>,
    private val sideEffects: List<SideEffect<Id, V>>,
    private val mutablePagingBuffer: MutablePagingBuffer<Id, K, V, E>,
    private val errorHandlingStrategy: ErrorHandlingStrategy,
    private val middleware: List<Middleware<K>>,
    private val fetchingStrategy: FetchingStrategy<Id, K, E>,
    private val pagingConfig: PagingConfig,
    private val initialLoadParams: PagingSource.LoadParams<K>,
    private val itemStore: Store<Id, V>,
    private val pageStore: Store<PagingSource.LoadParams<K>, PagingSource.LoadResult.Data<Id, K, V, E>>,
    private val errorFactory: ErrorFactory<E>,
    private val registry: KClassRegistry<Id, K, V, E>,
    initialState: PagingState<Id, E> = PagingState.initial()
) : Pager<Id, K, V, E> {

    private val coroutineScope = CoroutineScope(coroutineDispatcher)

    // TODO(): This is not thread safe!
    private val _mutablePagingState = MutableStateFlow(initialState)
    private val selfUpdatingItems = mutableMapOf<Id, SelfUpdatingItem<Id, V, E>>()

    private val appendLoadParamsQueue: LoadParamsQueue<K> = LoadParamsQueue()
    private val prependLoadParamsQueue: LoadParamsQueue<K> = LoadParamsQueue()

    init {
        handleLaunchEffects()
        handleEagerLoading()
    }

    @Composable
    override fun selfUpdatingItem(id: Id): SelfUpdatingItem<Id, V, E> {
        // TODO(): Make this thread safe

        LaunchedEffect(id) {
            fetchingStateHolder.updateMaxItemAccessedSoFar(id)

            if (selfUpdatingItems[id] == null) {

                val selfUpdatingItem = SelfUpdatingItem(
                    initialState = mutablePagingBuffer.get(id)?.value?.let {
                        ItemState.loaded<Id, V, E>(
                            it
                        )
                    } ?: ItemState.initial<Id, V, E>(),
                    store = itemStore,
                    errorFactory = errorFactory
                )

                selfUpdatingItems[id] = selfUpdatingItem
            }
        }

        return selfUpdatingItems[id]!!
    }

    @Composable
    override fun pagingState(loadParams: StateFlow<PagingSource.LoadParams<K>>): PagingState<Id, E> {

        val params by loadParams.collectAsState()

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

        val pagingState by _mutablePagingState.collectAsState()

        val transformedPagingState = pagingState.transform()

        return transformedPagingState
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

    private fun processAppendQueue() {

        while (appendLoadParamsQueue.isNotEmpty()) {
            val pagingState = _mutablePagingState.value
            val fetchingState = fetchingStateHolder.state.value

            if (fetchingStrategy.shouldFetch(
                    appendLoadParamsQueue.first(),
                    pagingState,
                    fetchingState
                )
            ) {
                val loadParams = appendLoadParamsQueue.removeFirst()

                if (pagingConfig.placeholdersEnabled) {
                    // Add placeholders to paging buffer
                    mutablePagingBuffer.append(
                        loadParams,
                        createPlaceholders(pagingConfig.pageSize, loadParams.key)
                    )
                }

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
            val loadResult = pageStore.fresh(loadParams)
            mutablePagingBuffer.prepend(loadParams, loadResult)

            // Update state
            updateStateWithPrependData(loadResult)

            // Clear prepend queue
            prependLoadParamsQueue.clear()

            loadResult.prevKey?.let {
                prependLoadParamsQueue.addLast(
                    PagingSource.LoadParams(
                        it,
                        PagingSource.LoadParams.Strategy.SkipCache,
                        PagingSource.LoadParams.Direction.Prepend
                    )
                )
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
            val loadResult = pageStore.fresh(loadParams)
            mutablePagingBuffer.append(loadParams, loadResult)

            // Update state
            updateStateWithAppendData(loadResult)

            // Load next key, if not null
            // TODO(): Design decision to skip cache on incremental loads
            loadResult.nextKey?.let {
                appendLoadParamsQueue.addLast(
                    PagingSource.LoadParams(
                        it,
                        PagingSource.LoadParams.Strategy.SkipCache,
                        PagingSource.LoadParams.Direction.Append
                    )
                )
            }

        } catch (pagingError: PagingError) {
            when (errorHandlingStrategy) {
                ErrorHandlingStrategy.Ignore -> {
                    // Ignore
                }

                ErrorHandlingStrategy.PassThrough -> {
                    // TODO(): Design decision to remove placeholders when passing an error through
                    mutablePagingBuffer.remove(loadParams)

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


    @Composable
    private fun PagingState<Id, E>.transform(): PagingState<Id, E> {
        val snapshot = mutablePagingBuffer.snapshotFrom(ids, ::onSnapshot)
        return snapshot.transform(this)
    }


    @Composable
    private fun ItemSnapshotList<Id, V>.transform(prevPagingState: PagingState<Id, E>): PagingState<Id, E> {

        var transformed by remember(this) { mutableStateOf(this) }


        transformations.forEach { transformation ->
            transformed = transformation.invoke(this, transformationParams)
        }

        val ids = remember(transformed) { transformed.getAll().map { it.id } }

        return PagingState(
            ids = ids,
            loadStates = prevPagingState.loadStates
        )
    }

    private fun onSnapshot(snapshot: ItemSnapshotList<Id, V>) {
        coroutineScope.launch {
            sideEffects.forEach { sideEffect -> sideEffect.invoke(snapshot) }
        }
    }

    private fun createPlaceholders(count: Int, key: K): PagingSource.LoadResult.Data<Id, K, V, E> {
        TODO()
    }

    private fun updateStateWithAppendData(data: PagingSource.LoadResult.Data<Id, K, V, E>) {
        val prevState = _mutablePagingState.value
        _mutablePagingState.value = PagingState(
            ids = prevState.ids + data.items.map { it.id },
            loadStates = prevState.loadStates.copy(
                append = PagingLoadState.NotLoading(data.nextKey == null)
            )
        )
    }

    private fun updateStateWithPrependData(data: PagingSource.LoadResult.Data<Id, K, V, E>) {
        val prevState = _mutablePagingState.value
        _mutablePagingState.value = PagingState(
            ids = data.items.map { it.id } + prevState.ids,
            loadStates = prevState.loadStates.copy(
                append = PagingLoadState.NotLoading(data.nextKey == null)
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