package org.mobilenativefoundation.paging.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.plus
import org.mobilenativefoundation.paging.core.impl.DefaultAggregatingStrategy
import org.mobilenativefoundation.paging.core.impl.DefaultFetchingStrategy
import org.mobilenativefoundation.paging.core.impl.DefaultLoadNextEffect
import org.mobilenativefoundation.paging.core.impl.DefaultLogger
import org.mobilenativefoundation.paging.core.impl.DefaultReducer
import org.mobilenativefoundation.paging.core.impl.Dispatcher
import org.mobilenativefoundation.paging.core.impl.EffectsHolder
import org.mobilenativefoundation.paging.core.impl.EffectsLauncher
import org.mobilenativefoundation.paging.core.impl.RealDispatcher
import org.mobilenativefoundation.paging.core.impl.RealMutablePagingBuffer
import org.mobilenativefoundation.paging.core.impl.RealPager
import org.mobilenativefoundation.paging.core.impl.RealQueueManager
import org.mobilenativefoundation.paging.core.impl.RetriesManager
import org.mobilenativefoundation.paging.core.impl.StateManager
import kotlin.reflect.KClass


data class PagingKey<out K : Any, out P : Any>(
    val key: K,
    val params: P? = null,
)

enum class PagingKeyType {
    SINGLE,
    COLLECTION
}


/**
 * An enum defining sorting options that can be applied during fetching.
 */
enum class Sort {
    NEWEST,
    OLDEST,
    ALPHABETICAL,
    REVERSE_ALPHABETICAL,
}

/**
 * Defines filters that can be applied during fetching.
 */
interface Filter<T : Any> {
    operator fun invoke(items: List<T>): List<T>
}

sealed interface PagingData<out Id : Any, out K : Any, out P : Any, out D : Any> {
    data class Single<Id : Any, K : Any, P : Any, D : Any>(
        val id: Id,
        val data: D
    ) : PagingData<Id, K, P, D>

    data class Collection<Id : Any, K : Any, P : Any, D : Any>(
        val items: List<Single<Id, K, P, D>>,
        val itemsBefore: Int?,
        val itemsAfter: Int?,
        val prevKey: PagingKey<K, P>,
        val nextKey: PagingKey<K, P>?,
    ) : PagingData<Id, K, P, D>
}


interface Pager<Id : Comparable<Id>, K : Any, P : Any, D : Any, E : Any, A : Any> {
    val state: StateFlow<PagingState<Id, K, P, D, E>>
    fun dispatch(action: PagingAction.User<Id, K, P, D, E, A>)
}

sealed interface PagingState<Id : Comparable<Id>, out K : Any, out P : Any, out D : Any, out E : Any> {
    val currentKey: PagingKey<K, P>
    val prefetchPosition: PagingKey<K, P>?

    data class Initial<Id : Comparable<Id>, K : Any, P : Any, D : Any, E : Any>(
        override val currentKey: PagingKey<K, P>,
        override val prefetchPosition: PagingKey<K, P>?
    ) : PagingState<Id, K, P, D, E>

    data class Loading<Id : Comparable<Id>, K : Any, P : Any, D : Any, E : Any>(
        override val currentKey: PagingKey<K, P>,
        override val prefetchPosition: PagingKey<K, P>?
    ) : PagingState<Id, K, P, D, E>

    sealed interface Error<Id : Comparable<Id>, out K : Any, out P : Any, out D : Any, out E : Any, out RE : Any> : PagingState<Id, K, P, D, E> {
        val error: RE

        data class Exception<Id : Comparable<Id>, K : Any, P : Any, D : Any, E : Any>(
            override val error: Throwable,
            override val currentKey: PagingKey<K, P>,
            override val prefetchPosition: PagingKey<K, P>?
        ) : Error<Id, K, P, D, E, Throwable>

        data class Custom<Id : Comparable<Id>, K : Any, P : Any, D : Any, E : Any>(
            override val error: E,
            override val currentKey: PagingKey<K, P>,
            override val prefetchPosition: PagingKey<K, P>?
        ) : Error<Id, K, P, D, E, E>
    }

    sealed interface Data<Id : Comparable<Id>, K : Any, P : Any, D : Any, E : Any> : PagingState<Id, K, P, D, E> {
        val data: List<PagingData.Single<Id, K, P, D>>
        val itemsBefore: Int?
        val itemsAfter: Int?
        val nextKey: PagingKey<K, P>?

        data class Idle<Id : Comparable<Id>, K : Any, P : Any, D : Any, E : Any>(
            override val data: List<PagingData.Single<Id, K, P, D>>,
            override val itemsBefore: Int?,
            override val itemsAfter: Int?,
            override val nextKey: PagingKey<K, P>?,
            override val currentKey: PagingKey<K, P>,
            override val prefetchPosition: PagingKey<K, P>?
        ) : Data<Id, K, P, D, E>

        data class LoadingMore<Id : Comparable<Id>, K : Any, P : Any, D : Any, E : Any>(
            override val data: List<PagingData.Single<Id, K, P, D>>,
            override val itemsBefore: Int?,
            override val itemsAfter: Int?,
            override val nextKey: PagingKey<K, P>?,
            override val currentKey: PagingKey<K, P>,
            override val prefetchPosition: PagingKey<K, P>?
        ) : Data<Id, K, P, D, E>

        data class ErrorLoadingMore<Id : Comparable<Id>, K : Any, P : Any, D : Any, RE : Any>(
            override val error: RE,
            override val data: List<PagingData.Single<Id, K, P, D>>,
            override val itemsBefore: Int?,
            override val itemsAfter: Int?,
            override val nextKey: PagingKey<K, P>?,
            override val currentKey: PagingKey<K, P>,
            override val prefetchPosition: PagingKey<K, P>?
        ) : Data<Id, K, P, D, Nothing>, Error<Id, K, P, D, Nothing, RE>
    }
}

sealed interface PagingAction<Id : Comparable<Id>, out K : Any, out P : Any, out D : Any, out E : Any, out A : Any> {
    sealed interface User<Id : Comparable<Id>, out K : Any, out P : Any, out D : Any, out E : Any, out A : Any> : PagingAction<Id, K, P, D, E, A> {
        data class Load<K : Any, P : Any>(
            val key: PagingKey<K, P>,
        ) : User<Nothing, K, P, Nothing, Nothing, Nothing>

        data class Custom<A : Any>(
            val action: A
        ) : User<Nothing, Nothing, Nothing, Nothing, Nothing, A>
    }

    data class App<Id : Comparable<Id>, K : Any, P : Any, D : Any, E : Any, A : Any>(
        val type: Type
    ) : PagingAction<Id, K, P, D, E, A> {
        enum class Type {
            START
        }
    }

    data class Load<Id : Comparable<Id>, K : Any, P : Any, D : Any, E : Any, A : Any>(
        val key: PagingKey<K, P>,
    ) : PagingAction<Id, K, P, D, E, A>

    data class UpdateData<Id : Comparable<Id>, K : Any, P : Any, D : Any, E : Any, A : Any>(
        val params: PagingSource.LoadParams<K, P>,
        val data: PagingSource.LoadResult.Data<Id, K, P, D>
    ) : PagingAction<Id, K, P, D, E, A>

    data class UpdateError<Id : Comparable<Id>, K : Any, P : Any, D : Any, E : Any, A : Any>(
        val params: PagingSource.LoadParams<K, P>,
        val error: PagingSource.LoadResult.Error<Id, K, P, D, E>
    ) : PagingAction<Id, K, P, D, E, A>

}


interface Middleware<Id : Comparable<Id>, K : Any, P : Any, D : Any, E : Any, A : Any> {
    fun apply(action: PagingAction<Id, K, P, D, E, A>, next: suspend (PagingAction<Id, K, P, D, E, A>) -> Unit)
}

interface Injector<T : Any> {
    fun inject(): T
}

interface OptionalInjector<T : Any> {
    fun inject(): T?
}

class RealInjector<T : Any> : Injector<T> {
    var instance: T? = null

    override fun inject(): T {
        return instance!!
    }
}


class RealOptionalInjector<T : Any> : OptionalInjector<T> {
    var instance: T? = null

    override fun inject(): T? {
        return instance
    }
}

class PagerBuilder<Id : Comparable<Id>, K : Any, P : Any, D : Any, E : Any, A : Any>(
    scope: CoroutineScope,
    initialState: PagingState<Id, K, P, D, E>,
    private val initialKey: PagingKey<K, P>,
    private val anchorPosition: StateFlow<PagingKey<K, P>>
) {

    private val childScope = scope + Job()

    private var middleware: MutableList<Middleware<Id, K, P, D, E, A>> = mutableListOf()

    private var pagingConfigInjector = RealInjector<PagingConfig>().apply {
        instance = PagingConfig(10, 50, InsertionStrategy.APPEND)
    }

    private var fetchingStrategyInjector = RealInjector<FetchingStrategy<Id, K, P, D>>()

    private val effectsHolder: EffectsHolder<Id, K, P, D, E, A> = EffectsHolder()

    private val dispatcherInjector = RealInjector<Dispatcher<Id, K, P, D, E, A>>()

    private val loggerInjector = RealOptionalInjector<Logger>()

    private val queueManagerInjector = RealInjector<QueueManager<K, P>>()
    private val pagingBufferInjector = RealInjector<PagingBuffer<Id, K, P, D>>()

    private val insertionStrategyInjector = RealInjector<InsertionStrategy>()

    private val stateManager = StateManager(initialState, loggerInjector)

    private var loadNextEffect: LoadNextEffect<Id, K, P, D, E, A> = DefaultLoadNextEffect<Id, K, P, D, E, A>(loggerInjector, queueManagerInjector)

    private lateinit var reducer: Reducer<Id, K, P, D, E, A>

    fun reducer(reducer: Reducer<Id, K, P, D, E, A>) = apply { this.reducer = reducer }

    fun defaultReducer(block: DefaultReducerBuilder<Id, K, P, D, E, A>.() -> Unit) = apply {
        val builder = DefaultReducerBuilder<Id, K, P, D, E, A>(
            childScope = childScope,
            initialKey = initialKey,
            dispatcherInjector = dispatcherInjector,
            loggerInjector = loggerInjector,
            pagingConfigInjector = pagingConfigInjector,
            anchorPosition = anchorPosition
        )
        block(builder)
        val reducer = builder.build()
        this.reducer = reducer
    }

    fun <PA : PagingAction<Id, K, P, D, E, A>, S : PagingState<Id, K, P, D, E>> effect(
        action: KClass<out PagingAction<Id, K, P, D, E, A>>,
        state: KClass<out PagingState<Id, K, P, D, E>>,
        effect: Effect<Id, K, P, D, E, A, PA, S>
    ) = apply {
        this.effectsHolder.put(action, state, effect)
    }

    fun loadNextEffect(effect: LoadNextEffect<Id, K, P, D, E, A>) = apply {
        this.loadNextEffect = effect
    }

    fun middleware(middleware: Middleware<Id, K, P, D, E, A>) = apply {
        this.middleware.add(middleware)
    }

    fun logger(logger: Logger) = apply { this.loggerInjector.instance = logger }

    fun defaultLogger() = apply { this.loggerInjector.instance = DefaultLogger() }

    fun pagingConfig(pagingConfig: PagingConfig) = apply { this.pagingConfigInjector.instance = pagingConfig }

    fun pagerBufferMaxSize(maxSize: Int) = apply { this.pagingBufferInjector.instance = RealMutablePagingBuffer<Id, K, P, D, E, A>(maxSize) }

    fun insertionStrategy(insertionStrategy: InsertionStrategy) = apply { this.insertionStrategyInjector.instance = insertionStrategy }

    private fun provideDefaultEffects() {
        this.effectsHolder.put(PagingAction.UpdateData::class, PagingState.Data.Idle::class, this.loadNextEffect)
    }

    private fun provideDispatcher() {
        val effectsLauncher = EffectsLauncher<Id, K, P, D, E, A>(effectsHolder)

        val dispatcher = RealDispatcher(
            stateManager = stateManager,
            middleware = middleware,
            reducer = reducer,
            effectsLauncher = effectsLauncher,
            childScope = childScope
        )

        dispatcherInjector.instance = dispatcher
    }

    private fun provideQueueManager() {
        val queueManager = RealQueueManager<Id, K, P, D, E, A>(
            pagingConfigInjector = pagingConfigInjector,
            loggerInjector = loggerInjector,
            dispatcherInjector = dispatcherInjector,
            fetchingStrategy = fetchingStrategyInjector.inject(),
            pagingBuffer = pagingBufferInjector.inject(),
            anchorPosition = anchorPosition,
            stateManager = stateManager
        )

        queueManagerInjector.instance = queueManager
    }

    fun build(): Pager<Id, K, P, D, E, A> {

        provideDefaultEffects()
        provideDispatcher()
        provideQueueManager()

        return RealPager(
            dispatcher = dispatcherInjector.inject(),
            stateManager = stateManager,
        )
    }
}

interface Reducer<Id : Comparable<Id>, K : Any, P : Any, D : Any, E : Any, A : Any> {
    suspend fun reduce(
        action: PagingAction<Id, K, P, D, E, A>,
        state: PagingState<Id, K, P, D, E>
    ): PagingState<Id, K, P, D, E>
}

interface Logger {
    fun log(message: String)
}

interface Effect<Id : Comparable<Id>, K : Any, P : Any, D : Any, E : Any, A : Any, PA : PagingAction<Id, K, P, D, E, A>, S : PagingState<Id, K, P, D, E>> {
    operator fun invoke(action: PA, state: S, dispatch: (PagingAction<Id, K, P, D, E, A>) -> Unit)
}

data class PagingConfig(
    val pageSize: Int,
    val prefetchDistance: Int,
    val insertionStrategy: InsertionStrategy
)


class DefaultReducerBuilder<Id : Comparable<Id>, K : Any, P : Any, D : Any, E : Any, A : Any>(
    private val initialKey: PagingKey<K, P>,
    private val childScope: CoroutineScope,
    private val dispatcherInjector: Injector<Dispatcher<Id, K, P, D, E, A>>,
    private val loggerInjector: OptionalInjector<Logger>,
    private val pagingConfigInjector: Injector<PagingConfig>,
    private val anchorPosition: StateFlow<PagingKey<K, P>>,
) {
    private var errorHandlingStrategy: ErrorHandlingStrategy = ErrorHandlingStrategy.RetryLast()
    private var aggregatingStrategy: AggregatingStrategy<Id, K, P, D> = DefaultAggregatingStrategy()
    private var fetchingStrategy: FetchingStrategy<Id, K, P, D> = DefaultFetchingStrategy()
    private var customActionReducer: UserCustomActionReducer<Id, K, P, D, E, A>? = null
    private var pagingBufferMaxSize: Int = 100

    fun errorHandlingStrategy(errorHandlingStrategy: ErrorHandlingStrategy) = apply { this.errorHandlingStrategy = errorHandlingStrategy }
    fun aggregatingStrategy(aggregatingStrategy: AggregatingStrategy<Id, K, P, D>) = apply { this.aggregatingStrategy = aggregatingStrategy }
    fun fetchingStrategy(fetchingStrategy: FetchingStrategy<Id, K, P, D>) = apply { this.fetchingStrategy = fetchingStrategy }
    fun customActionReducer(customActionReducer: UserCustomActionReducer<Id, K, P, D, E, A>) = apply { this.customActionReducer = customActionReducer }
    fun pagingBufferMaxSize(maxSize: Int) = apply { this.pagingBufferMaxSize = maxSize }

    fun build(): Reducer<Id, K, P, D, E, A> {
        val mutablePagingBuffer = mutablePagingBufferOf<Id, K, P, D, E, A>(maxSize = this.pagingBufferMaxSize)

        return DefaultReducer(
            childScope = childScope,
            dispatcherInjector = dispatcherInjector,
            pagingConfigInjector = pagingConfigInjector,
            userCustomActionReducer = customActionReducer,
            anchorPosition = anchorPosition,
            loggerInjector = loggerInjector,
            mutablePagingBuffer = mutablePagingBuffer,
            aggregatingStrategy = aggregatingStrategy,
            initialKey = initialKey,
            retriesManager = RetriesManager(),
            errorHandlingStrategy = errorHandlingStrategy
        )
    }
}


interface FetchingStrategy<Id : Comparable<Id>, K : Any, P : Any, D : Any> {
    fun shouldFetch(
        anchorPosition: PagingKey<K, P>,
        prefetchPosition: PagingKey<K, P>?,
        pagingConfig: PagingConfig,
        pagingBuffer: PagingBuffer<Id, K, P, D>,
    ): Boolean
}


/**
 * A custom data structure for efficiently accessing paging data.
 */
interface PagingBuffer<Id : Comparable<Id>, K : Any, P : Any, D : Any> {
    fun get(params: PagingSource.LoadParams<K, P>): PagingSource.LoadResult.Data<Id, K, P, D>?

    fun get(key: PagingKey<K, P>): PagingSource.LoadResult.Data<Id, K, P, D>?

    fun head(): PagingSource.LoadResult.Data<Id, K, P, D>?

    fun getAll(): List<PagingSource.LoadResult.Data<Id, K, P, D>>

    fun isEmpty(): Boolean

    fun indexOf(key: PagingKey<K, P>): Int
}

interface PagingSource<Id : Comparable<Id>, K : Any, P : Any, D : Any, E : Any> {
    fun stream(params: LoadParams<K, P>): Flow<LoadResult<Id, K, P, D, E>>

    data class LoadParams<K : Any, P : Any>(
        val key: PagingKey<K, P>,
        val refresh: Boolean,
    )

    sealed class LoadResult<Id : Comparable<Id>, out K : Any, out P : Any, out D : Any, out E : Any> {
        sealed class Error<Id : Comparable<Id>, K : Any, P : Any, D : Any, E : Any> : LoadResult<Id, K, P, D, E>() {
            data class Exception<Id : Comparable<Id>, K : Any, P : Any, D : Any, E : Any>(val error: Throwable) : Error<Id, K, P, D, E>()
            data class Custom<Id : Comparable<Id>, K : Any, P : Any, D : Any, E : Any>(val error: E) : Error<Id, K, P, D, E>()
        }

        data class Data<Id : Comparable<Id>, K : Any, P : Any, D : Any>(val collection: PagingData.Collection<Id, K, P, D>) : LoadResult<Id, K, P, D, Nothing>()
    }
}


interface AggregatingStrategy<Id : Comparable<Id>, K : Any, P : Any, D : Any> {
    fun aggregate(
        anchorPosition: PagingKey<K, P>,
        prefetchPosition: PagingKey<K, P>?,
        pagingConfig: PagingConfig,
        pagingBuffer: PagingBuffer<Id, K, P, D>,
    ): PagingItems<Id, K, P, D>
}

data class PagingItems<Id : Comparable<Id>, K : Any, P : Any, D : Any>(
    val data: List<PagingData.Single<Id, K, P, D>>
)

sealed interface ErrorHandlingStrategy {
    data object Ignore : ErrorHandlingStrategy
    data object PassThrough : ErrorHandlingStrategy
    data class RetryLast(
        val maxRetries: Int = 3
    ) : ErrorHandlingStrategy
}

interface UserCustomActionReducer<Id : Comparable<Id>, K : Any, P : Any, D : Any, E : Any, A : Any> {
    fun reduce(
        action: PagingAction.User.Custom<A>,
        state: PagingState<Id, K, P, D, E>
    ): PagingState<Id, K, P, D, E>
}

interface QueueManager<K : Any, P : Any> {
    fun enqueue(key: PagingKey<K, P>)
}

interface MutablePagingBuffer<Id : Comparable<Id>, K : Any, P : Any, D : Any> : PagingBuffer<Id, K, P, D> {
    fun put(params: PagingSource.LoadParams<K, P>, page: PagingSource.LoadResult.Data<Id, K, P, D>)
}


inline fun <Id : Comparable<Id>, K : Any, P : Any, D : Any, E : Any, A : Any> mutablePagingBufferOf(maxSize: Int): MutablePagingBuffer<Id, K, P, D> {
    return RealMutablePagingBuffer<Id, K, P, D, E, A>(maxSize)
}

enum class InsertionStrategy {
    APPEND,
    PREPEND,
    REPLACE,
}

interface PagingSourceCollector<Id : Comparable<Id>, K : Any, P : Any, D : Any, E : Any, A : Any> {
    suspend operator fun invoke(
        params: PagingSource.LoadParams<K, P>,
        results: Flow<PagingSource.LoadResult<Id, K, P, D, E>>,
        state: PagingState<Id, K, P, D, E>,
        dispatch: (action: PagingAction<Id, K, P, D, E, A>) -> Unit
    )
}

interface PagingSourceStreamProvider<Id : Comparable<Id>, K : Any, P : Any, D : Any, E : Any> {
    fun provide(params: PagingSource.LoadParams<K, P>): Flow<PagingSource.LoadResult<Id, K, P, D, E>>
}

typealias LoadNextEffect<Id, K, P, D, E, A> = Effect<Id, K, P, D, E, A, PagingAction.UpdateData<Id, K, P, D, E, A>, PagingState.Data.Idle<Id, K, P, D, E>>
