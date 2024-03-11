package org.mobilenativefoundation.paging.core.impl

import co.touchlab.kermit.CommonWriter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.mobilenativefoundation.paging.core.AggregatingStrategy
import org.mobilenativefoundation.paging.core.Effect
import org.mobilenativefoundation.paging.core.ErrorHandlingStrategy
import org.mobilenativefoundation.paging.core.FetchingStrategy
import org.mobilenativefoundation.paging.core.Injector
import org.mobilenativefoundation.paging.core.InsertionStrategy
import org.mobilenativefoundation.paging.core.LoadNextEffect
import org.mobilenativefoundation.paging.core.Logger
import org.mobilenativefoundation.paging.core.Middleware
import org.mobilenativefoundation.paging.core.MutablePagingBuffer
import org.mobilenativefoundation.paging.core.OptionalInjector
import org.mobilenativefoundation.paging.core.Pager
import org.mobilenativefoundation.paging.core.PagingAction
import org.mobilenativefoundation.paging.core.PagingBuffer
import org.mobilenativefoundation.paging.core.PagingConfig
import org.mobilenativefoundation.paging.core.PagingData
import org.mobilenativefoundation.paging.core.PagingItems
import org.mobilenativefoundation.paging.core.PagingKey
import org.mobilenativefoundation.paging.core.PagingSource
import org.mobilenativefoundation.paging.core.PagingSourceCollector
import org.mobilenativefoundation.paging.core.PagingSourceStreamProvider
import org.mobilenativefoundation.paging.core.PagingState
import org.mobilenativefoundation.paging.core.QueueManager
import org.mobilenativefoundation.paging.core.Reducer
import org.mobilenativefoundation.paging.core.UserCustomActionReducer
import org.mobilenativefoundation.store.store5.ExperimentalStoreApi
import org.mobilenativefoundation.store.store5.MutableStore
import org.mobilenativefoundation.store.store5.StoreReadRequest
import org.mobilenativefoundation.store.store5.StoreReadResponse
import kotlin.reflect.KClass

const val UNCHECKED_CAST = "UNCHECKED_CAST"

interface Dispatcher<Id : Comparable<Id>, K : Any, P : Any, D : Any, E : Any, A : Any> {
    fun <PA : PagingAction<Id, K, P, D, E, A>> dispatch(action: PA, index: Int = 0)
}

class StateManager<Id : Comparable<Id>, K : Any, P : Any, D : Any, E : Any>(
    initialState: PagingState<Id, K, P, D, E>,
    loggerInjector: OptionalInjector<Logger>
) {

    private val logger = loggerInjector.inject()

    private val _state = MutableStateFlow(initialState)
    val state = _state.asStateFlow()

    fun update(nextState: PagingState<Id, K, P, D, E>) {

        log(nextState)

        _state.value = nextState
    }

    private fun log(nextState: PagingState<Id, K, P, D, E>) {
        logger?.log(
            """
            Updating state:
                Previous state: ${_state.value}
                Next state: $nextState
        """.trimIndent()
        )
    }
}


class RealPager<Id : Comparable<Id>, K : Any, P : Any, D : Any, E : Any, A : Any>(
    initialKey: PagingKey<K, P>,
    stateManager: StateManager<Id, K, P, D, E>,
    private val dispatcher: Dispatcher<Id, K, P, D, E, A>,
) : Pager<Id, K, P, D, E, A> {

    init {
        dispatcher.dispatch(PagingAction.Load(initialKey))
    }

    override val state: StateFlow<PagingState<Id, K, P, D, E>> = stateManager.state

    override fun dispatch(action: PagingAction.User<Id, K, P, D, E, A>) {
        dispatcher.dispatch(action)
    }
}

typealias PagingActionToEffects<Id, K, P, D, E, A> = MutableMap<KClass<out PagingAction<Id, K, P, D, E, A>>, MutableList<Effect<Id, K, P, D, E, A, *, *>>>
typealias PagingStateToPagingActionToEffects<Id, K, P, D, E, A> = MutableMap<KClass<out PagingState<Id, K, P, D, E>>, PagingActionToEffects<Id, K, P, D, E, A>>


@Suppress(UNCHECKED_CAST)
class EffectsHolder<Id : Comparable<Id>, K : Any, P : Any, D : Any, E : Any, A : Any> {
    private val effects: PagingStateToPagingActionToEffects<Id, K, P, D, E, A> = mutableMapOf()

    fun <PA : PagingAction<Id, K, P, D, E, A>, S : PagingState<Id, K, P, D, E>> get(
        action: KClass<out PagingAction<Id, K, P, D, E, A>>,
        state: KClass<out PagingState<Id, K, P, D, E>>
    ): List<Effect<Id, K, P, D, E, A, PA, S>> {
        action as KClass<PA>
        state as KClass<S>
        return effects[state]?.get(action) as? List<Effect<Id, K, P, D, E, A, PA, S>> ?: emptyList()
    }

    fun <PA : PagingAction<Id, K, P, D, E, A>, S : PagingState<Id, K, P, D, E>> put(
        action: KClass<out PagingAction<*, *, *, *, *, *>>,
        state: KClass<out PagingState<*, *, *, *, *>>,
        effect: Effect<Id, K, P, D, E, A, PA, S>
    ) {
        action as KClass<out PA>
        state as KClass<out S>

        if (state !in effects) {
            effects[state] = mutableMapOf()
        }

        if (action !in effects[state]!!) {
            effects[state]!![action] = mutableListOf()
        }

        effects[state]!![action]!!.add(effect)
    }
}

class EffectsLauncher<Id : Comparable<Id>, K : Any, P : Any, D : Any, E : Any, A : Any>(
    private val effectsHolder: EffectsHolder<Id, K, P, D, E, A>
) {

    fun <PA : PagingAction<Id, K, P, D, E, A>, S : PagingState<Id, K, P, D, E>> launch(action: PA, state: S, dispatch: (PagingAction<Id, K, P, D, E, A>) -> Unit) {
        val effects = effectsHolder.get<PA, S>(action::class, state::class)

        effects.forEach { effect -> effect(action, state, dispatch) }
    }
}


class RealDispatcher<Id : Comparable<Id>, K : Any, P : Any, D : Any, E : Any, A : Any>(
    private val stateManager: StateManager<Id, K, P, D, E>,
    private val middleware: List<Middleware<Id, K, P, D, E, A>>,
    private val reducer: Reducer<Id, K, P, D, E, A>,
    private val effectsLauncher: EffectsLauncher<Id, K, P, D, E, A>,
    private val childScope: CoroutineScope,
) : Dispatcher<Id, K, P, D, E, A> {

    override fun <PA : PagingAction<Id, K, P, D, E, A>> dispatch(action: PA, index: Int) {
        if (index < middleware.size) {
            middleware[index].apply(action) { nextAction ->
                dispatch(nextAction, index + 1)
            }
        } else {
            childScope.launch {
                reduceAndLaunchEffects(action)
            }
        }
    }

    private suspend fun <PA : PagingAction<Id, K, P, D, E, A>> reduceAndLaunchEffects(action: PA) {
        val prevState = stateManager.state.value
        val nextState = reducer.reduce(action, prevState)

        stateManager.update(nextState)

        when (nextState) {
            is PagingState.Initial -> effectsLauncher.launch(action, nextState, ::dispatch)
            is PagingState.Data.Idle -> effectsLauncher.launch(action, nextState, ::dispatch)
            is PagingState.Data.ErrorLoadingMore<Id, K, P, D, *> -> effectsLauncher.launch(action, nextState, ::dispatch)
            is PagingState.Data.LoadingMore -> effectsLauncher.launch(action, nextState, ::dispatch)
            is PagingState.Error.Custom -> effectsLauncher.launch(action, nextState, ::dispatch)
            is PagingState.Error.Exception -> effectsLauncher.launch(action, nextState, ::dispatch)
            is PagingState.Loading -> effectsLauncher.launch(action, nextState, ::dispatch)
        }
    }
}


class DefaultLogger : Logger {
    override fun log(message: String) {
        logger.d(
            """
            
            $message
            
            """.trimIndent(),
        )
    }

    private val logger =
        co.touchlab.kermit.Logger.apply {
            setLogWriters(listOf(CommonWriter()))
            setTag("Paging")
        }
}

class RealQueueManager<Id : Comparable<Id>, K : Any, P : Any, D : Any, E : Any, A : Any>(
    pagingConfigInjector: Injector<PagingConfig>,
    loggerInjector: OptionalInjector<Logger>,
    dispatcherInjector: Injector<Dispatcher<Id, K, P, D, E, A>>,
    private val fetchingStrategy: FetchingStrategy<Id, K, P, D>,
    private val pagingBuffer: PagingBuffer<Id, K, P, D>,
    private val anchorPosition: StateFlow<PagingKey<K, P>>,
    private val stateManager: StateManager<Id, K, P, D, E>,
) : QueueManager<K, P> {

    private val logger = loggerInjector.inject()
    private val pagingConfig = pagingConfigInjector.inject()
    private val dispatcher = dispatcherInjector.inject()

    private val queue: ArrayDeque<PagingKey<K, P>> = ArrayDeque()

    override fun enqueue(key: PagingKey<K, P>) {
        logger?.log(
            """
            Enqueueing:
                Key: $key
        """.trimIndent()
        )

        queue.addLast(key)

        processQueue()
    }

    private fun processQueue() {
        while (queue.isNotEmpty() && fetchingStrategy.shouldFetch(
                anchorPosition = anchorPosition.value,
                prefetchPosition = stateManager.state.value.prefetchPosition,
                pagingConfig = pagingConfig,
                pagingBuffer = pagingBuffer,
            )
        ) {
            val nextKey = queue.removeFirst()

            logger?.log(
                """Dequeued:
                    Key: $nextKey
                """.trimMargin(),
            )

            dispatcher.dispatch(PagingAction.Load(nextKey))
        }
    }
}


class RealMutablePagingBuffer<Id : Comparable<Id>, K : Any, P : Any, D : Any, E : Any, A : Any>(
    private val maxSize: Int
) : MutablePagingBuffer<Id, K, P, D> {

    private val buffer: Array<PagingSource.LoadResult.Data<Id, K, P, D>?> = arrayOfNulls(maxSize)
    private val paramsToIndex: MutableMap<PagingSource.LoadParams<K, P>, Int> = mutableMapOf()
    private val keyToIndex: MutableMap<PagingKey<K, P>, Int> = mutableMapOf()
    private var head = 0
    private var tail = 0
    private var size = 0


    override fun get(params: PagingSource.LoadParams<K, P>): PagingSource.LoadResult.Data<Id, K, P, D>? {
        val index = paramsToIndex[params]
        return if (index != null) buffer[index] else null
    }

    override fun get(key: PagingKey<K, P>): PagingSource.LoadResult.Data<Id, K, P, D>? {
        val index = keyToIndex[key]
        return if (index != null) buffer[index] else null
    }

    override fun put(params: PagingSource.LoadParams<K, P>, page: PagingSource.LoadResult.Data<Id, K, P, D>) {
        if (size == maxSize) {
            val oldestIndex = head
            val oldestParams = paramsToIndex.entries.first { it.value == oldestIndex }.key
            paramsToIndex.remove(oldestParams)
            keyToIndex.remove(oldestParams.key)
            buffer[oldestIndex] = null
            head = (head + 1) % maxSize
        }
        val index = tail
        buffer[index] = page
        paramsToIndex[params] = index
        keyToIndex[params.key] = index
        tail = (tail + 1) % maxSize
        size = minOf(size + 1, maxSize)
    }

    override fun head(): PagingSource.LoadResult.Data<Id, K, P, D>? {
        return buffer[head]
    }

    override fun getAll(): List<PagingSource.LoadResult.Data<Id, K, P, D>> {
        val pages = mutableListOf<PagingSource.LoadResult.Data<Id, K, P, D>>()
        var index = head
        var count = 0
        while (count < size) {
            val page = buffer[index]
            if (page != null) {
                pages.add(page)
            }
            index = (index + 1) % maxSize
            count++
        }
        return pages
    }

    override fun isEmpty(): Boolean = size == 0

    override fun indexOf(key: PagingKey<K, P>): Int = buffer.filterNotNull().flatMap { it.collection.items }.indexOfFirst { it.id == key.key }
}


class DefaultLoadNextEffect<Id : Comparable<Id>, K : Any, P : Any, D : Any, E : Any, A : Any>(
    loggerInjector: OptionalInjector<Logger>,
    queueManagerInjector: Injector<QueueManager<K, P>>
) : LoadNextEffect<Id, K, P, D, E, A> {

    private val logger = loggerInjector.inject()
    private val queueManager = queueManagerInjector.inject()

    override fun invoke(action: PagingAction.UpdateData<Id, K, P, D, E, A>, state: PagingState.Data.Idle<Id, K, P, D, E>, dispatch: (PagingAction<Id, K, P, D, E, A>) -> Unit) {
        logger?.log(
            """
            Running post reducer effect:
                Effect: Load next
                State: $state
                Action: $action
            """.trimIndent(),
        )

        action.data.collection.nextKey?.key?.let {
            queueManager.enqueue(action.data.collection.nextKey)
        }
    }
}

interface JobCoordinator {
    fun launchIfNotActive(key: Any, block: suspend CoroutineScope.() -> Unit)
    fun cancel(key: Any)
    fun cancelAll()
}

class RealJobCoordinator(
    private val childScope: CoroutineScope
) : JobCoordinator {
    private val jobs: MutableMap<Any, Job> = mutableMapOf()

    override fun launchIfNotActive(
        key: Any,
        block: suspend CoroutineScope.() -> Unit,
    ) {
        if (jobs[key]?.isActive != true) {
            val job =
                childScope.launch {
                    block()
                }
            jobs[key] = job

            job.invokeOnCompletion {
                cancel(key)
            }
        }
    }

    override fun cancel(key: Any) {
        jobs[key]?.cancel()
        jobs.remove(key)
    }

    override fun cancelAll() {
        jobs.keys.forEach { cancel(it) }
    }
}


class DefaultAggregatingStrategy<Id : Comparable<Id>, K : Any, P : Any, D : Any> : AggregatingStrategy<Id, K, P, D> {
    override fun aggregate(anchorPosition: PagingKey<K, P>, prefetchPosition: PagingKey<K, P>?, pagingConfig: PagingConfig, pagingBuffer: PagingBuffer<Id, K, P, D>): PagingItems<Id, K, P, D> {
        if (pagingBuffer.isEmpty()) return PagingItems(emptyList())

        val orderedItems = mutableListOf<PagingData.Single<Id, K, P, D>>()

        var currentPage: PagingSource.LoadResult.Data<Id, K, P, D>? = pagingBuffer.head()

        while (currentPage != null) {
            when (pagingConfig.insertionStrategy) {
                InsertionStrategy.APPEND -> orderedItems.addAll(currentPage.collection.items)
                InsertionStrategy.PREPEND -> orderedItems.addAll(0, currentPage.collection.items)
                InsertionStrategy.REPLACE -> {
                    orderedItems.clear()
                    orderedItems.addAll(currentPage.collection.items)
                }
            }

            currentPage = currentPage.collection.nextKey?.let { pagingBuffer.get(it) }
        }

        return PagingItems(orderedItems)
    }
}

class DefaultFetchingStrategy<Id : Comparable<Id>, K : Any, P : Any, D : Any> : FetchingStrategy<Id, K, P, D> {
    override fun shouldFetch(anchorPosition: PagingKey<K, P>, prefetchPosition: PagingKey<K, P>?, pagingConfig: PagingConfig, pagingBuffer: PagingBuffer<Id, K, P, D>): Boolean {
        if (prefetchPosition == null) return true

        val indexOfAnchor = pagingBuffer.indexOf(anchorPosition)
        val indexOfPrefetch = pagingBuffer.indexOf(prefetchPosition)

        if (indexOfAnchor == -1 && indexOfPrefetch == -1 || indexOfPrefetch == -1) return true
        return indexOfPrefetch - indexOfAnchor < pagingConfig.prefetchDistance
    }

}


class DefaultReducer<Id : Comparable<Id>, K : Any, P : Any, D : Any, E : Any, A : Any>(
    private val childScope: CoroutineScope,
    private val initialKey: PagingKey<K, P>,
    private val dispatcherInjector: Injector<Dispatcher<Id, K, P, D, E, A>>,
    pagingConfigInjector: Injector<PagingConfig>,
    private val userCustomActionReducer: UserCustomActionReducer<Id, K, P, D, E, A>?,
    private val anchorPosition: StateFlow<PagingKey<K, P>>,
    loggerInjector: OptionalInjector<Logger>,
    private val errorHandlingStrategy: ErrorHandlingStrategy,
    private val mutablePagingBuffer: MutablePagingBuffer<Id, K, P, D>,
    private val aggregatingStrategy: AggregatingStrategy<Id, K, P, D>,
    private val retriesManager: RetriesManager<Id, K, P, D>
) : Reducer<Id, K, P, D, E, A> {

    private val logger = loggerInjector.inject()
    private val pagingConfig = pagingConfigInjector.inject()
    private val dispatcher = dispatcherInjector.inject()

    override suspend fun reduce(action: PagingAction<Id, K, P, D, E, A>, state: PagingState<Id, K, P, D, E>): PagingState<Id, K, P, D, E> {
        logger?.log(
            """
            Reducing:
                Action: $action
                Previous state: $state
            """.trimIndent(),
        )

        return when (action) {
            is PagingAction.UpdateData -> reduceUpdateDataAction(action, state)
            is PagingAction.User.Custom -> reduceUserCustomAction(action, state)
            is PagingAction.User.Load -> reduceUserLoadAction(action, state)
            is PagingAction.UpdateError -> reduceUpdateErrorAction(action, state)
            is PagingAction.Load -> reduceLoadAction(action, state)
        }
    }

    private fun reduceUpdateDataAction(action: PagingAction.UpdateData<Id, K, P, D, E, A>, prevState: PagingState<Id, K, P, D, E>): PagingState<Id, K, P, D, E> {
        mutablePagingBuffer.put(action.params, action.data)

        val nextPagingItems = aggregatingStrategy.aggregate(
            anchorPosition = anchorPosition.value,
            prefetchPosition = prevState.prefetchPosition,
            pagingConfig = pagingConfig,
            pagingBuffer = mutablePagingBuffer
        )

        resetRetriesFor(action.params)

        val nextPrefetchPosition = action.data.collection.items.last().id

        return PagingState.Data.Idle(
            data = nextPagingItems.data,
            itemsBefore = action.data.collection.itemsBefore,
            itemsAfter = action.data.collection.itemsAfter,
            currentKey = action.data.collection.prevKey,
            nextKey = action.data.collection.nextKey,
            prefetchPosition = action.data.collection.prevKey
        )

    }

    private suspend fun reduceUpdateErrorAction(action: PagingAction.UpdateError<Id, K, P, D, E, A>, prevState: PagingState<Id, K, P, D, E>): PagingState<Id, K, P, D, E> {
        return when (errorHandlingStrategy) {
            ErrorHandlingStrategy.Ignore -> prevState
            ErrorHandlingStrategy.PassThrough -> reduceUpdateErrorActionWithPassThrough(action, prevState)
            is ErrorHandlingStrategy.RetryLast -> retryLast(errorHandlingStrategy.maxRetries, action, prevState)
        }
    }

    private suspend fun retryLast(maxRetries: Int, action: PagingAction.UpdateError<Id, K, P, D, E, A>, prevState: PagingState<Id, K, P, D, E>): PagingState<Id, K, P, D, E> {
        val retries = retriesManager.getRetriesFor(action.params)

        return if (retries < maxRetries) {
            // Retry without emitting the error

            retriesManager.incrementRetriesFor(action.params)
            dispatcher.dispatch(PagingAction.Load(action.params.key))
            prevState
        } else {
            // Emit the error and reset the counter

            retriesManager.resetRetriesFor(action.params)
            if (prevState is PagingState.Data) {
                PagingState.Data.ErrorLoadingMore(
                    error = action.error,
                    data = prevState.data,
                    itemsBefore = prevState.itemsBefore,
                    itemsAfter = prevState.itemsAfter,
                    nextKey = prevState.nextKey,
                    currentKey = prevState.currentKey,
                    prefetchPosition = prevState.prefetchPosition
                )
            } else {
                when (action.error) {
                    is PagingSource.LoadResult.Error.Custom -> PagingState.Error.Custom(action.error.error, action.params.key, prevState.prefetchPosition)
                    is PagingSource.LoadResult.Error.Exception -> PagingState.Error.Exception(action.error.error, action.params.key, prevState.prefetchPosition)
                }
            }
        }
    }

    private fun reduceUpdateErrorActionWithPassThrough(action: PagingAction.UpdateError<Id, K, P, D, E, A>, prevState: PagingState<Id, K, P, D, E>): PagingState<Id, K, P, D, E> {
        // Emitting it, but not doing anything else

        val errorState: PagingState.Error<Id, K, P, D, E, *> = when (action.error) {
            is PagingSource.LoadResult.Error.Custom -> PagingState.Error.Custom(action.error.error, action.params.key, prevState.prefetchPosition)
            is PagingSource.LoadResult.Error.Exception -> PagingState.Error.Exception(action.error.error, action.params.key, prevState.prefetchPosition)
        }

        return if (prevState is PagingState.Data) {
            PagingState.Data.ErrorLoadingMore(
                error = errorState,
                data = prevState.data,
                itemsBefore = prevState.itemsBefore,
                itemsAfter = prevState.itemsAfter,
                currentKey = prevState.currentKey,
                nextKey = prevState.nextKey,
                prefetchPosition = prevState.prefetchPosition,
            )
        } else {
            errorState
        }
    }


    private fun reduceUserCustomAction(action: PagingAction.User.Custom<A>, prevState: PagingState<Id, K, P, D, E>): PagingState<Id, K, P, D, E> {
        return userCustomActionReducer?.reduce(action, prevState) ?: prevState
    }

    private fun reduceLoadActionAndDataState(prevState: PagingState.Data<Id, K, P, D, E>) = PagingState.Data.LoadingMore<Id, K, P, D, E>(
        data = prevState.data,
        itemsBefore = prevState.itemsBefore,
        itemsAfter = prevState.itemsAfter,
        currentKey = prevState.currentKey,
        nextKey = prevState.nextKey,
        prefetchPosition = prevState.prefetchPosition
    )

    private fun reduceLoadActionAndNonDataState(key: PagingKey<K, P>, prevState: PagingState<Id, K, P, D, E>) = PagingState.Loading<Id, K, P, D, E>(
        currentKey = key,
        prefetchPosition = prevState.prefetchPosition
    )

    private fun reduceLoadAction(action: PagingAction.Load<Id, K, P, D, E, A>, prevState: PagingState<Id, K, P, D, E>): PagingState<Id, K, P, D, E> {
        return if (prevState is PagingState.Data) reduceLoadActionAndDataState(prevState) else reduceLoadActionAndNonDataState(action.key, prevState)
    }


    private fun reduceUserLoadAction(action: PagingAction.User.Load<K, P>, prevState: PagingState<Id, K, P, D, E>): PagingState<Id, K, P, D, E> {
        return if (prevState is PagingState.Data) reduceLoadActionAndDataState(prevState) else reduceLoadActionAndNonDataState(action.key, prevState)
    }

    private fun resetRetriesFor(params: PagingSource.LoadParams<K, P>) {
        childScope.launch {
            retriesManager.resetRetriesFor(params)
        }
    }
}


class RetriesManager<Id : Comparable<Id>, K : Any, P : Any, D : Any> {
    private val retries = mutableMapOf<PagingSource.LoadParams<K, P>, Int>()

    private val mutexForRetries = Mutex()

    suspend fun resetRetriesFor(params: PagingSource.LoadParams<K, P>) {
        mutexForRetries.withLock { retries[params] = 0 }
    }

    suspend fun getRetriesFor(params: PagingSource.LoadParams<K, P>): Int {
        val count = mutexForRetries.withLock {
            retries[params] ?: 0
        }

        return count
    }

    suspend fun incrementRetriesFor(params: PagingSource.LoadParams<K, P>) {
        mutexForRetries.withLock {
            val prevCount = retries[params] ?: 0
            val nextCount = prevCount + 1
            retries[params] = nextCount
        }
    }
}

class DefaultPagingSourceCollector<Id : Comparable<Id>, K : Any, P : Any, D : Any, E : Any, A : Any> : PagingSourceCollector<Id, K, P, D, E, A> {
    override suspend fun invoke(
        params: PagingSource.LoadParams<K, P>,
        results: Flow<PagingSource.LoadResult<Id, K, P, D, E>>,
        state: PagingState<Id, K, P, D, E>,
        dispatch: (action: PagingAction<Id, K, P, D, E, A>) -> Unit
    ) {
        results.collect { result ->
            when (result) {
                is PagingSource.LoadResult.Data -> dispatch(PagingAction.UpdateData(params, result))
                is PagingSource.LoadResult.Error -> dispatch(PagingAction.UpdateError(params, result))
            }
        }
    }
}

class DefaultPagingSource<Id : Comparable<Id>, K : Any, P : Any, D : Any, E : Any, A : Any>(
    private val streamProvider: PagingSourceStreamProvider<Id, K, P, D, E>
) : PagingSource<Id, K, P, D, E> {
    private val streams = mutableMapOf<PagingKey<K, P>, Flow<PagingSource.LoadResult<Id, K, P, D, E>>>()

    override fun stream(params: PagingSource.LoadParams<K, P>): Flow<PagingSource.LoadResult<Id, K, P, D, E>> {
        if (params.key !in streams) {
            streams[params.key] = streamProvider.provide(params)
        }
        return streams[params.key]!!
    }
}


@OptIn(ExperimentalStoreApi::class)
fun <Id : Comparable<Id>, K : Any, P : Any, D : Any, E : Any, A : Any> MutableStore<PagingKey<K, P>, PagingData<Id, K, P, D>>.pagingSourceStreamProvider(
    keyFactory: StorePagingSourceKeyFactory<Id, K, P, D>
): PagingSourceStreamProvider<Id, K, P, D, E> {

    fun createParentStream(key: PagingKey<K, P>) = paged<Id, K, P, D, E>(key)

    fun createChildStream(key: PagingKey<K, P>) = stream<Any>(StoreReadRequest.cached(key, refresh = false))

    return StorePagingSourceStreamProvider(::createParentStream, ::createChildStream, keyFactory)
}

@OptIn(ExperimentalStoreApi::class)
fun <Id : Comparable<Id>, K : Any, P : Any, D : Any, E : Any> MutableStore<PagingKey<K, P>, PagingData<Id, K, P, D>>.paged(
    key: PagingKey<K, P>
): Flow<PagingSource.LoadResult<Id, K, P, D, E>> = stream<Any>(StoreReadRequest.cached(key, refresh = false)).mapNotNull { response ->
    when (response) {
        is StoreReadResponse.Data -> PagingSource.LoadResult.Data(response.value as PagingData.Collection)
        is StoreReadResponse.Error.Exception -> PagingSource.LoadResult.Error.Exception(response.error)
        is StoreReadResponse.Error.Message -> PagingSource.LoadResult.Error.Exception(Exception(response.message))
        is StoreReadResponse.Loading,
        is StoreReadResponse.NoNewData -> null
    }
}

class StorePagingSourceStreamProvider<Id : Comparable<Id>, K : Any, P : Any, D : Any, E : Any>(
    private val createParentStream: (key: PagingKey<K, P>) -> Flow<PagingSource.LoadResult<Id, K, P, D, E>>,
    private val createChildStream: (key: PagingKey<K, P>) -> Flow<StoreReadResponse<PagingData<Id, K, P, D>>>,
    private val keyFactory: StorePagingSourceKeyFactory<Id, K, P, D>
) : PagingSourceStreamProvider<Id, K, P, D, E> {
    private val pages: MutableMap<PagingKey<K, P>, PagingSource.LoadResult.Data<Id, K, P, D>> = mutableMapOf()
    private val mutexForPages = Mutex()

    override fun provide(params: PagingSource.LoadParams<K, P>): Flow<PagingSource.LoadResult<Id, K, P, D, E>> =
        createParentStream(params.key).map { result ->
            when (result) {
                is PagingSource.LoadResult.Data -> {
                    mutexForPages.withLock {
                        pages[params.key] = result
                    }

                    var data = result

                    result.collection.items.forEach { child ->
                        val childKey = keyFactory.createKeyFor(child)
                        initAndCollectChildStream(child, childKey, params.key) { updatedData -> data = updatedData }
                    }

                    data
                }

                is PagingSource.LoadResult.Error -> result
            }
        }

    private fun initAndCollectChildStream(
        data: PagingData.Single<Id, K, P, D>,
        key: PagingKey<K, P>,
        parentKey: PagingKey<K, P>,
        emit: (updatedData: PagingSource.LoadResult.Data<Id, K, P, D>) -> Unit
    ) {
        createChildStream(key).distinctUntilChanged().onEach { response ->

            if (response is StoreReadResponse.Data) {
                val updatedValue = response.value

                if (updatedValue is PagingData.Single) {
                    mutexForPages.withLock {
                        pages[parentKey]!!.let { currentData ->
                            val updatedItems = currentData.collection.items.toMutableList()
                            val indexOfChild = updatedItems.indexOfFirst { it.id == data.id }
                            val child = updatedItems[indexOfChild]
                            if (child != updatedValue) {
                                updatedItems[indexOfChild] = updatedValue

                                val updatedPage = currentData.copy(collection = currentData.collection.copy(items = updatedItems))

                                pages[parentKey] = updatedPage

                                emit(updatedPage)
                            }
                        }
                    }
                }
            }
        }
    }
}

fun interface StorePagingSourceKeyFactory<Id : Comparable<Id>, K : Any, P : Any, D : Any> {
    fun createKeyFor(single: PagingData.Single<Id, K, P, D>): PagingKey<K, P>
}