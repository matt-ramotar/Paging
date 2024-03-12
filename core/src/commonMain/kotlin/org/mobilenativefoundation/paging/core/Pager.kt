package org.mobilenativefoundation.paging.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.plus
import org.mobilenativefoundation.paging.core.PagingAction.Load
import org.mobilenativefoundation.paging.core.PagingAction.UpdateData
import org.mobilenativefoundation.paging.core.PagingAction.User
import org.mobilenativefoundation.paging.core.PagingSource.LoadResult
import org.mobilenativefoundation.paging.core.PagingState.Data
import org.mobilenativefoundation.paging.core.PagingState.Error
import org.mobilenativefoundation.paging.core.PagingState.Initial
import org.mobilenativefoundation.paging.core.PagingState.Loading
import org.mobilenativefoundation.paging.core.impl.DefaultAggregatingStrategy
import org.mobilenativefoundation.paging.core.impl.DefaultAppLoadEffect
import org.mobilenativefoundation.paging.core.impl.DefaultFetchingStrategy
import org.mobilenativefoundation.paging.core.impl.DefaultLoadNextEffect
import org.mobilenativefoundation.paging.core.impl.DefaultLogger
import org.mobilenativefoundation.paging.core.impl.DefaultPagingSource
import org.mobilenativefoundation.paging.core.impl.DefaultPagingSourceCollector
import org.mobilenativefoundation.paging.core.impl.DefaultReducer
import org.mobilenativefoundation.paging.core.impl.DefaultUserLoadEffect
import org.mobilenativefoundation.paging.core.impl.DefaultUserLoadMoreEffect
import org.mobilenativefoundation.paging.core.impl.Dispatcher
import org.mobilenativefoundation.paging.core.impl.EffectsHolder
import org.mobilenativefoundation.paging.core.impl.EffectsLauncher
import org.mobilenativefoundation.paging.core.impl.JobCoordinator
import org.mobilenativefoundation.paging.core.impl.RealDispatcher
import org.mobilenativefoundation.paging.core.impl.RealJobCoordinator
import org.mobilenativefoundation.paging.core.impl.RealMutablePagingBuffer
import org.mobilenativefoundation.paging.core.impl.RealPager
import org.mobilenativefoundation.paging.core.impl.RealQueueManager
import org.mobilenativefoundation.paging.core.impl.RetriesManager
import org.mobilenativefoundation.paging.core.impl.StateManager
import org.mobilenativefoundation.paging.core.impl.StorePagingSourceKeyFactory
import org.mobilenativefoundation.paging.core.impl.pagingSourceStreamProvider
import org.mobilenativefoundation.store.store5.ExperimentalStoreApi
import org.mobilenativefoundation.store.store5.MutableStore
import org.mobilenativefoundation.store.store5.Store
import kotlin.reflect.KClass


data class PagingKey<out K : Any, out P : Any>(
    val key: K,
    val params: P,
)

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

/**
 * [Pager] is responsible for coordinating the paging process and providing access to the paging state and data.
 * This is the main entry point for the [org.mobilenativefoundation.paging] library.
 *
 * @param Id The type of the unique identifier for each item in the paged data.
 * @param K The type of the key used for paging.
 * @param P The type of the parameters associated with each page of data.
 * @param D The type of the data items.
 * @param E The type of errors that can occur during the paging process.
 * @param A The type of custom actions that can be dispatched to modify the paging state.
 */
interface Pager<Id : Comparable<Id>, K : Any, P : Any, D : Any, E : Any, A : Any> {
    /**
     * The current paging state exposed as a [StateFlow].
     * The paging state represents the current state of the paging data, including loaded pages, errors, and loading status.
     * Observers can collect this flow to react to changes in the paging state.
     */
    val state: StateFlow<PagingState<Id, K, P, D, E>>

    /**
     * Dispatches a user-initiated [PagingAction] to modify the paging state.
     *
     * User actions can be dispatched to trigger specific behaviors or modifications to the paging state.
     * The dispatched action will go through the configured [Middleware] chain and [Reducer] before updating the paging state.
     * After updating the state, the dispatched action will launch each configured post-reducer [Effect].
     *
     * @param action The user-initiated [PagingAction] to dispatch.
     */
    fun dispatch(action: User<Id, K, P, D, E, A>)
}


/**
 * Represents the current state of the paging data.
 *
 * The paging state can be in different stages, such as [Initial], [Loading], [Error], or [Data].
 * It can contain the current key, prefetch position, errors, and data, such as loaded items and the next key.
 *
 * @param Id The type of the unique identifier for each item in the paged data.
 * @param K The type of the key used for paging.
 * @param P The type of the parameters associated with each page of data.
 * @param D The type of the data items.
 * @param E The type of errors that can occur during the paging process.
 */
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

        data class ErrorLoadingMore<Id : Comparable<Id>, K : Any, P : Any, D : Any, E : Any, RE : Any>(
            override val error: RE,
            override val data: List<PagingData.Single<Id, K, P, D>>,
            override val itemsBefore: Int?,
            override val itemsAfter: Int?,
            override val nextKey: PagingKey<K, P>?,
            override val currentKey: PagingKey<K, P>,
            override val prefetchPosition: PagingKey<K, P>?
        ) : Data<Id, K, P, D, E>, Error<Id, K, P, D, E, RE>
    }
}

/**
 * Defines the actions that can be dispatched to modify the paging state.
 *
 * @param Id The type of the unique identifier for each item in the paged data.
 * @param K The type of the key used for paging.
 * @param P The type of the parameters associated with each page of data.
 * @param D The type of the data items.
 * @param E The type of errors that can occur during the paging process.
 * @param A The type of custom actions that can be dispatched to modify the paging state.
 */
sealed interface PagingAction<Id : Comparable<Id>, out K : Any, out P : Any, out D : Any, out E : Any, out A : Any> {

    /**
     * Defines user-initiated actions.
     */
    sealed interface User<Id : Comparable<Id>, out K : Any, out P : Any, out D : Any, out E : Any, out A : Any> : PagingAction<Id, K, P, D, E, A> {

        /**
         * Represents a user-initiated action to load data for a specific page key.
         *
         * @param key The page key to load data for.
         */
        data class Load<Id : Comparable<Id>, out K : Any, out P : Any, out D : Any, out E : Any, out A : Any>(
            val key: PagingKey<K, P>,
        ) : User<Id, K, P, D, E, A>

        /**
         * Represents a custom user-initiated action.
         *
         * @param action The custom action payload.
         */
        data class Custom<Id : Comparable<Id>, out K : Any, out P : Any, out D : Any, out E : Any, out A : Any>(
            val action: A
        ) : User<Id, K, P, D, E, A>
    }


    /**
     * Represents an app-initiated action to load data for a specific page key.
     *
     * @param key The page key to load data for.
     */
    data class Load<Id : Comparable<Id>, K : Any, P : Any, D : Any, E : Any, A : Any>(
        val key: PagingKey<K, P>,
    ) : PagingAction<Id, K, P, D, E, A>

    /**
     * Represents an app-initiated action to update the paging state with loaded data.
     *
     * @param params The parameters associated with the loaded data.
     * @param data The loaded data.
     */
    data class UpdateData<Id : Comparable<Id>, K : Any, P : Any, D : Any, E : Any, A : Any>(
        val params: PagingSource.LoadParams<K, P>,
        val data: PagingSource.LoadResult.Data<Id, K, P, D>
    ) : PagingAction<Id, K, P, D, E, A>

    /**
     * Represents an app-initiated action to update the paging state with an error.
     *
     * @param params The parameters associated with the error.
     * @param error The error that occurred.
     */
    data class UpdateError<Id : Comparable<Id>, K : Any, P : Any, D : Any, E : Any, A : Any>(
        val params: PagingSource.LoadParams<K, P>,
        val error: PagingSource.LoadResult.Error<Id, K, P, D, E>
    ) : PagingAction<Id, K, P, D, E, A>

}

/**
 * Represents a middleware that intercepts and modifies paging actions before they reach the reducer.
 *
 * [Middleware] allows for pre-processing, logging, or any other custom logic to be applied to actions before they are handled by the [Reducer].
 * It can also modify or replace the action before passing it to the next [Middleware] or [Reducer].
 *
 * @param Id The type of the unique identifier for each item in the paged data.
 * @param K The type of the key used for paging.
 * @param P The type of the parameters associated with each page of data.
 * @param D The type of the data items.
 * @param E The type of errors that can occur during the paging process.
 * @param A The type of custom actions that can be dispatched to modify the paging state.
 */
interface Middleware<Id : Comparable<Id>, K : Any, P : Any, D : Any, E : Any, A : Any> {

    /**
     * Applies the middleware logic to the given [action].
     *
     * The middleware can perform any necessary pre-processing, logging, or modification of the action
     * before invoking the [next] function to pass the action to the next middleware or the reducer.
     *
     * @param action The paging action to be processed by the middleware.
     * @param next A suspending function that should be invoked with the processed action to pass it to the next middleware or the reducer.
     * If the middleware does not want to pass the action further, it can choose not to invoke this function.
     */
    suspend fun apply(action: PagingAction<Id, K, P, D, E, A>, next: suspend (PagingAction<Id, K, P, D, E, A>) -> Unit)
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


/**
 * A builder class for creating a [Pager] instance.
 * The [PagerBuilder] enables configuring the paging behavior,
 * such as the initial state, initial key, anchor position, middleware, effects, reducer, logger, and paging config.
 *
 * @param Id The type of the unique identifier for each item in the paged data.
 * @param K The type of the key used for paging.
 * @param P The type of the parameters associated with each page of data.
 * @param D The type of the data items.
 * @param E The type of errors that can occur during the paging process.
 * @param A The type of custom actions that can be dispatched.
 * @param scope The [CoroutineScope] in which the paging operations will be performed.
 * @param initialState The initial [PagingState] of the pager.
 * @param initialKey The initial [PagingKey] of the pager.
 * @param anchorPosition A [StateFlow] representing the anchor position for paging.
 */
class PagerBuilder<Id : Comparable<Id>, K : Any, P : Any, D : Any, E : Any, A : Any>(
    scope: CoroutineScope,
    initialState: PagingState<Id, K, P, D, E>,
    private val initialKey: PagingKey<K, P>,
    private val anchorPosition: StateFlow<PagingKey<K, P>>
) {

    private val childScope = scope + Job()
    private val jobCoordinator = RealJobCoordinator(childScope)

    private var middleware: MutableList<Middleware<Id, K, P, D, E, A>> = mutableListOf()

    private var pagingConfigInjector = RealInjector<PagingConfig>().apply {
        instance = PagingConfig(10, 50, InsertionStrategy.APPEND)
    }

    private var fetchingStrategyInjector = RealInjector<FetchingStrategy<Id, K, P, D>>().apply {
        this.instance = DefaultFetchingStrategy()
    }

    private var pagingBufferMaxSize = 100

    private val effectsHolder: EffectsHolder<Id, K, P, D, E, A> = EffectsHolder()

    private val dispatcherInjector = RealInjector<Dispatcher<Id, K, P, D, E, A>>()

    private val loggerInjector = RealOptionalInjector<Logger>()

    private val queueManagerInjector = RealInjector<QueueManager<K, P>>()
    private val mutablePagingBufferInjector = RealInjector<MutablePagingBuffer<Id, K, P, D>>().apply {
        this.instance = mutablePagingBufferOf<Id, K, P, D, E, A>(500)
    }

    private val insertionStrategyInjector = RealInjector<InsertionStrategy>()
    private val pagingSourceCollectorInjector = RealInjector<PagingSourceCollector<Id, K, P, D, E, A>>().apply {
        this.instance = DefaultPagingSourceCollector()
    }
    private val pagingSourceInjector = RealInjector<PagingSource<Id, K, P, D, E>>()

    private val stateManager = StateManager(initialState, loggerInjector)

    private var loadNextEffect: LoadNextEffect<Id, K, P, D, E, A> = DefaultLoadNextEffect(loggerInjector, queueManagerInjector)

    private var appLoadEffect: AppLoadEffect<Id, K, P, D, E, A> = DefaultAppLoadEffect(
        loggerInjector = loggerInjector,
        dispatcherInjector = dispatcherInjector,
        jobCoordinator = jobCoordinator,
        pagingSourceCollectorInjector = pagingSourceCollectorInjector,
        pagingSourceInjector = pagingSourceInjector,
        stateManager = stateManager
    )

    private var userLoadEffect: UserLoadEffect<Id, K, P, D, E, A> = DefaultUserLoadEffect(
        loggerInjector = loggerInjector,
        dispatcherInjector = dispatcherInjector,
        jobCoordinator = jobCoordinator,
        pagingSourceCollectorInjector = pagingSourceCollectorInjector,
        pagingSourceInjector = pagingSourceInjector,
        stateManager = stateManager
    )

    private var userLoadMoreEffect: UserLoadMoreEffect<Id, K, P, D, E, A> = DefaultUserLoadMoreEffect(
        loggerInjector = loggerInjector,
        dispatcherInjector = dispatcherInjector,
        jobCoordinator = jobCoordinator,
        pagingSourceCollectorInjector = pagingSourceCollectorInjector,
        pagingSourceInjector = pagingSourceInjector,
        stateManager = stateManager
    )

    private lateinit var reducer: Reducer<Id, K, P, D, E, A>

    /**
     * Sets the [Reducer] for the pager.
     *
     * @param reducer The [Reducer] to be used for reducing paging actions and state.
     * @return The [PagerBuilder] instance for chaining.
     */
    fun reducer(reducer: Reducer<Id, K, P, D, E, A>) = apply { this.reducer = reducer }

    /**
     * Configures the default [Reducer] using the provided [DefaultReducerBuilder].
     *
     * @param block A lambda function that takes a [DefaultReducerBuilder] as receiver and allows configuring the default reducer.
     * @return The [PagerBuilder] instance for chaining.
     */
    fun defaultReducer(
        block: DefaultReducerBuilder<Id, K, P, D, E, A>.() -> Unit
    ) = apply {
        val builder = DefaultReducerBuilder<Id, K, P, D, E, A>(
            childScope = childScope,
            initialKey = initialKey,
            dispatcherInjector = dispatcherInjector,
            loggerInjector = loggerInjector,
            pagingConfigInjector = pagingConfigInjector,
            anchorPosition = anchorPosition,
            mutablePagingBufferInjector = mutablePagingBufferInjector,
            jobCoordinator = jobCoordinator
        )
        block(builder)
        val reducer = builder.build()
        this.reducer = reducer
    }

    /**
     * Adds an [Effect] to be invoked after reducing the state for the specified [PagingAction] and [PagingState] types.
     *
     * @param PA The type of the [PagingAction] that triggers the effect.
     * @param S The type of the [PagingState] that triggers the effect.
     * @param action The [KClass] of the [PagingAction] that triggers the effect.
     * @param state The [KClass] of the [PagingState] that triggers the effect.
     * @param effect The [Effect] to be invoked.
     * @return The [PagerBuilder] instance for chaining.
     */
    fun <PA : PagingAction<Id, K, P, D, E, A>, S : PagingState<Id, K, P, D, E>> effect(
        action: KClass<out PagingAction<*, *, *, *, *, *>>,
        state: KClass<out PagingState<*, *, *, *, *>>,
        effect: Effect<Id, K, P, D, E, A, PA, S>
    ) = apply {
        this.effectsHolder.put(action, state, effect)
    }

    /**
     * Sets the [LoadNextEffect] for the pager.
     *
     * @param effect The [LoadNextEffect] to be used for loading the next page of data.
     * @return The [PagerBuilder] instance for chaining.
     */
    fun loadNextEffect(effect: LoadNextEffect<Id, K, P, D, E, A>) = apply { this.loadNextEffect = effect }

    fun appLoadEffect(effect: AppLoadEffect<Id, K, P, D, E, A>) = apply { this.appLoadEffect = effect }
    fun userLoadEffect(effect: UserLoadEffect<Id, K, P, D, E, A>) = apply { this.userLoadEffect = effect }
    fun userLoadMoreEffect(effect: UserLoadMoreEffect<Id, K, P, D, E, A>) = apply { this.userLoadMoreEffect = effect }

    /**
     * Adds a [Middleware] to the pager.
     *
     * @param middleware The [Middleware] to be added.
     * @return The [PagerBuilder] instance for chaining.
     */
    fun middleware(middleware: Middleware<Id, K, P, D, E, A>) = apply {
        this.middleware.add(middleware)
    }

    /**
     * Sets the [Logger] for the pager.
     *
     * @param logger The [Logger] to be used for logging.
     * @return The [PagerBuilder] instance for chaining.
     */
    fun logger(logger: Logger) = apply { this.loggerInjector.instance = logger }

    /**
     * Sets the default [Logger] for the pager.
     *
     * @return The [PagerBuilder] instance for chaining.
     */
    fun defaultLogger() = apply { this.loggerInjector.instance = DefaultLogger() }

    /**
     * Sets the [PagingConfig] for the pager.
     *
     * @param pagingConfig The [PagingConfig] to be used for configuring the paging behavior.
     * @return The [PagerBuilder] instance for chaining.
     */
    fun pagingConfig(pagingConfig: PagingConfig) = apply { this.pagingConfigInjector.instance = pagingConfig }

    /**
     * Sets the maximum size of the pager buffer.
     *
     * @param maxSize The maximum size of the pager buffer.
     * @return The [PagerBuilder] instance for chaining.
     */
    fun pagerBufferMaxSize(maxSize: Int) = apply { this.mutablePagingBufferInjector.instance = RealMutablePagingBuffer<Id, K, P, D, E, A>(maxSize) }

    /**
     * Sets the [InsertionStrategy] for the pager.
     *
     * @param insertionStrategy The [InsertionStrategy] to be used for inserting new data into the pager buffer.
     * @return The [PagerBuilder] instance for chaining.
     */
    fun insertionStrategy(insertionStrategy: InsertionStrategy) = apply { this.insertionStrategyInjector.instance = insertionStrategy }

    fun pagingSourceCollector(pagingSourceCollector: PagingSourceCollector<Id, K, P, D, E, A>) = apply { this.pagingSourceCollectorInjector.instance = pagingSourceCollector }

    fun pagingSource(pagingSource: PagingSource<Id, K, P, D, E>) = apply { this.pagingSourceInjector.instance = pagingSource }

    fun defaultPagingSource(streamProvider: PagingSourceStreamProvider<Id, K, P, D, E>) = apply {
        this.pagingSourceInjector.instance = DefaultPagingSource<Id, K, P, D, E, A>(streamProvider)
    }

    @OptIn(ExperimentalStoreApi::class)
    fun mutableStorePagingSource(store: MutableStore<PagingKey<K, P>, PagingData<Id, K, P, D>>, factory: () -> StorePagingSourceKeyFactory<Id, K, P, D>) = apply {
        this.pagingSourceInjector.instance = DefaultPagingSource<Id, K, P, D, E, A>(
            streamProvider = store.pagingSourceStreamProvider<Id, K, P, D, E, A>(
                keyFactory = factory()
            )
        )
    }

    fun storePagingSource(store: Store<PagingKey<K, P>, PagingData<Id, K, P, D>>, factory: () -> StorePagingSourceKeyFactory<Id, K, P, D>) = apply {
        this.pagingSourceInjector.instance = DefaultPagingSource<Id, K, P, D, E, A>(
            streamProvider = store.pagingSourceStreamProvider<Id, K, P, D, E, A>(
                keyFactory = factory()
            )
        )
    }

    private fun provideDefaultEffects() {
        this.effectsHolder.put(UpdateData::class, Data.Idle::class, this.loadNextEffect)
        this.effectsHolder.put(Load::class, PagingState::class, this.appLoadEffect)
        this.effectsHolder.put(User.Load::class, Loading::class, this.userLoadEffect)
        this.effectsHolder.put(User.Load::class, Data.LoadingMore::class, this.userLoadMoreEffect)
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
            pagingBuffer = mutablePagingBufferInjector.inject(),
            anchorPosition = anchorPosition,
            stateManager = stateManager
        )

        queueManagerInjector.instance = queueManager
    }

    /**
     * Builds and returns the [Pager] instance.
     *
     * @return The created [Pager] instance.
     */
    fun build(): Pager<Id, K, P, D, E, A> {

        provideDefaultEffects()
        provideDispatcher()
        provideQueueManager()

        return RealPager(
            initialKey = initialKey,
            dispatcher = dispatcherInjector.inject(),
            pagingConfigInjector = pagingConfigInjector,
            stateManager = stateManager,
        )
    }
}

/**
 * The [Reducer] is responsible for taking the current [PagingState] and a dispatched [PagingAction],
 * and producing a new [PagingState] based on the action and the current state.
 *
 * @param Id The type of the unique identifier for each item in the paged data.
 * @param K The type of the key used for paging.
 * @param P The type of the parameters associated with each page of data.
 * @param D The type of the data items.
 * @param E The type of errors that can occur during the paging process.
 * @param A The type of custom actions that can be dispatched to modify the paging state.
 */
interface Reducer<Id : Comparable<Id>, K : Any, P : Any, D : Any, E : Any, A : Any> {
    /**
     * Reduces the current [PagingState] based on the dispatched [PagingAction] and returns a new [PagingState].
     *
     * This function is called whenever a [PagingAction] is dispatched to update the paging state.
     * It should handle the action and produce a new state based on the current state and the action.
     *
     * @param action The dispatched [PagingAction] to be reduced.
     * @param state The current [PagingState] before applying the action.
     * @return The new [PagingState] after applying the action to the current state.
     */
    suspend fun reduce(
        action: PagingAction<Id, K, P, D, E, A>,
        state: PagingState<Id, K, P, D, E>
    ): PagingState<Id, K, P, D, E>
}

interface Logger {
    fun log(message: String)
}

/**
 * Represents an effect that can be triggered after reducing a specific [PagingAction] and [PagingState] combination.
 *
 * Effects are side effects or additional actions that need to be performed after the state has been reduced based on a dispatched action.
 * They can be used for tasks such as loading more data, updating the UI, triggering network requests, or any other side effects that depend on the current state and action.
 *
 * @param Id The type of the unique identifier for each item in the paged data.
 * @param K The type of the key used for paging.
 * @param P The type of the parameters associated with each page of data.
 * @param D The type of the data items.
 * @param E The type of errors that can occur during the paging process.
 * @param A The type of custom actions that can be dispatched to modify the paging state.
 * @param PA The specific type of [PagingAction] that triggers this effect.
 * @param S The specific type of [PagingState] that triggers this effect.
 */
interface Effect<Id : Comparable<Id>, K : Any, P : Any, D : Any, E : Any, A : Any, PA : PagingAction<Id, K, P, D, E, A>, S : PagingState<Id, K, P, D, E>> {
    operator fun invoke(action: PA, state: S, dispatch: (PagingAction<Id, K, P, D, E, A>) -> Unit)
}

data class PagingConfig(
    val pageSize: Int,
    val prefetchDistance: Int,
    val insertionStrategy: InsertionStrategy
)


/**
 * A builder class for creating a default [Reducer] instance.
 *
 * It enables configuring error handling strategy, aggregating strategy, fetching strategy, custom action reducer, and paging buffer size.
 *
 * @param Id The type of the unique identifier for each item in the paged data.
 * @param K The type of the key used for paging.
 * @param P The type of the parameters associated with each page of data.
 * @param D The type of the data items.
 * @param E The type of errors that can occur during the paging process.
 * @param A The type of custom actions that can be dispatched to modify the paging state.
 * @param initialKey The initial [PagingKey] used as the starting point for paging.
 * @param childScope The [CoroutineScope] in which the reducer will operate.
 * @param dispatcherInjector The [Injector] used to provide the [Dispatcher] instance.
 * @param loggerInjector The [OptionalInjector] used to provide the optional [Logger] instance.
 * @param pagingConfigInjector The [Injector] used to provide the [PagingConfig] instance.
 * @param anchorPosition The [StateFlow] representing the anchor position for paging.
 */
class DefaultReducerBuilder<Id : Comparable<Id>, K : Any, P : Any, D : Any, E : Any, A : Any> internal constructor(
    private val initialKey: PagingKey<K, P>,
    private val childScope: CoroutineScope,
    private val dispatcherInjector: Injector<Dispatcher<Id, K, P, D, E, A>>,
    private val loggerInjector: OptionalInjector<Logger>,
    private val pagingConfigInjector: Injector<PagingConfig>,
    private val anchorPosition: StateFlow<PagingKey<K, P>>,
    private val mutablePagingBufferInjector: Injector<MutablePagingBuffer<Id, K, P, D>>,
    private val jobCoordinator: JobCoordinator
) {

    private var errorHandlingStrategy: ErrorHandlingStrategy = ErrorHandlingStrategy.RetryLast()
    private var aggregatingStrategy: AggregatingStrategy<Id, K, P, D> = DefaultAggregatingStrategy()
    private var fetchingStrategy: FetchingStrategy<Id, K, P, D> = DefaultFetchingStrategy()
    private var customActionReducer: UserCustomActionReducer<Id, K, P, D, E, A>? = null

    /**
     * Sets the [ErrorHandlingStrategy] to be used by the reducer.
     *
     * @param errorHandlingStrategy The [ErrorHandlingStrategy] to be used.
     * @return The [DefaultReducerBuilder] instance for chaining.
     */
    fun errorHandlingStrategy(errorHandlingStrategy: ErrorHandlingStrategy) = apply { this.errorHandlingStrategy = errorHandlingStrategy }

    /**
     * Sets the [AggregatingStrategy] to be used by the reducer.
     *
     * @param aggregatingStrategy The [AggregatingStrategy] to be used.
     * @return The [DefaultReducerBuilder] instance for chaining.
     */
    fun aggregatingStrategy(aggregatingStrategy: AggregatingStrategy<Id, K, P, D>) = apply { this.aggregatingStrategy = aggregatingStrategy }

    /**
     * Sets the [FetchingStrategy] to be used by the reducer.
     *
     * @param fetchingStrategy The [FetchingStrategy] to be used.
     * @return The [DefaultReducerBuilder] instance for chaining.
     */
    fun fetchingStrategy(fetchingStrategy: FetchingStrategy<Id, K, P, D>) = apply { this.fetchingStrategy = fetchingStrategy }

    /**
     * Sets the custom action reducer to be used by the reducer.
     *
     * @param customActionReducer The [UserCustomActionReducer] to be used.
     * @return The [DefaultReducerBuilder] instance for chaining.
     */
    fun customActionReducer(customActionReducer: UserCustomActionReducer<Id, K, P, D, E, A>) = apply { this.customActionReducer = customActionReducer }

    /**
     * Builds and returns the configured default [Reducer] instance.
     *
     * @return The built default [Reducer] instance.
     */
    fun build(): Reducer<Id, K, P, D, E, A> {
        val mutablePagingBuffer = mutablePagingBufferInjector.inject()

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
            errorHandlingStrategy = errorHandlingStrategy,
            jobCoordinator = jobCoordinator
        )
    }
}


/**
 * Represents a strategy for determining whether to fetch more data based on the current state of the pager.
 * The fetching strategy is responsible for deciding whether to fetch more data based on the anchor position,
 * prefetch position, paging configuration, and the current state of the paging buffer.
 *
 * Implementing a custom [FetchingStrategy] allows you to define your own logic for when to fetch more data.
 * For example, you can fetch more data when the user scrolls near the end of the currently loaded data, or when a certain number of items are remaining in the buffer.
 *
 * @param Id The type of the unique identifier for each item in the paged data.
 * @param K The type of the key used for paging.
 * @param P The type of the parameters associated with each page of data.
 * @param D The type of the data items.
 */
interface FetchingStrategy<Id : Comparable<Id>, K : Any, P : Any, D : Any> {

    /**
     * Determines whether to fetch more data based on the current state of the pager.
     * The [shouldFetch] implementation should determine whether more data should be fetched based on the provided parameters.
     *
     * @param anchorPosition The current anchor position in the paged data.
     * @param prefetchPosition The position to prefetch data from, or `null` if no prefetching is needed.
     * @param pagingConfig The configuration of the pager, including page size and prefetch distance.
     * @param pagingBuffer The current state of the paging buffer, containing the loaded data.
     * @return `true` if more data should be fetched, `false` otherwise.
     */
    fun shouldFetch(
        anchorPosition: PagingKey<K, P>,
        prefetchPosition: PagingKey<K, P>?,
        pagingConfig: PagingConfig,
        pagingBuffer: PagingBuffer<Id, K, P, D>,
    ): Boolean
}


/**
 * A custom data structure for efficiently storing and retrieving paging data.
 *
 * The [PagingBuffer] is responsible for caching and providing access to the loaded pages of data.
 * It allows retrieving data by load parameters, page key, or accessing the entire buffer.
 *
 * @param Id The type of the unique identifier for each item in the paged data.
 * @param K The type of the key used for paging.
 * @param P The type of the parameters associated with each page of data.
 * @param D The type of the data items.
 */
interface PagingBuffer<Id : Comparable<Id>, K : Any, P : Any, D : Any> {
    /**
     * Retrieves the data associated with the specified [PagingSource.LoadParams].
     *
     * @param params The [PagingSource.LoadParams] to retrieve the data for.
     * @return The [PagingSource.LoadResult.Data] associated with the specified [params], or `null` if not found.
     */
    fun get(params: PagingSource.LoadParams<K, P>): PagingSource.LoadResult.Data<Id, K, P, D>?

    /**
     * Retrieves the data associated with the specified [PagingKey].
     *
     * @param key The [PagingKey] to retrieve the data for.
     * @return The [PagingSource.LoadResult.Data] associated with the specified [key], or `null` if not found.
     */
    fun get(key: PagingKey<K, P>): PagingSource.LoadResult.Data<Id, K, P, D>?

    /**
     * Retrieves the data at the head of the buffer.
     *
     * @return The [PagingSource.LoadResult.Data] at the head of the buffer, or `null` if the buffer is empty.
     */
    fun head(): PagingSource.LoadResult.Data<Id, K, P, D>?

    /**
     * Retrieves all the data in the buffer as a list.
     *
     * @return A list of all the [PagingSource.LoadResult.Data] in the buffer.
     */
    fun getAll(): List<PagingSource.LoadResult.Data<Id, K, P, D>>

    /**
     * Checks if the buffer is empty.
     *
     * @return `true` if the buffer is empty, `false` otherwise.
     */
    fun isEmpty(): Boolean

    /**
     * Returns the index of the data associated with the specified [PagingKey] in the buffer.
     *
     * @param key The [PagingKey] to find the index for.
     * @return The index of the data associated with the specified [key], or -1 if not found.
     */
    fun indexOf(key: PagingKey<K, P>): Int
}

/**
 * Represents a data source that provides paged data.
 *
 * A [PagingSource] is responsible for loading pages of data from a specific data source,
 * such as a database or a network API. It emits a stream of [LoadResult] instances that
 * represent the loaded data or any errors that occurred during loading.
 *
 * @param Id The type of the unique identifier for each item in the paged data.
 * @param K The type of the key used for paging.
 * @param P The type of the parameters associated with each page of data.
 * @param D The type of the data items.
 * @param E The type of custom errors that can occur during loading.
 */
interface PagingSource<Id : Comparable<Id>, K : Any, P : Any, D : Any, E : Any> {
    /**
     * Returns a flow of [LoadResult] instances for the specified [LoadParams].
     *
     * This function is called by the paging library to load pages of data. It takes the
     * [LoadParams] as input and returns a flow of [LoadResult] instances representing
     * the loaded data or any errors that occurred.
     *
     * @param params The [LoadParams] specifying the page key and refresh state.
     * @return A flow of [LoadResult] instances representing the loaded data or errors.
     */
    fun stream(params: LoadParams<K, P>): Flow<LoadResult<Id, K, P, D, E>>

    /**
     * Represents the parameters for loading a page of data.
     *
     * @param K The type of the key used for paging.
     * @param P The type of the parameters associated with each page of data.
     * @property key The [PagingKey] identifying the page to load.
     * @property refresh Indicates whether to refresh the data or load a new page.
     */
    data class LoadParams<K : Any, P : Any>(
        val key: PagingKey<K, P>,
        val refresh: Boolean,
    )

    /**
     * Represents the result of loading a page of data.
     *
     * @param Id The type of the unique identifier for each item in the paged data.
     * @param K The type of the key used for paging.
     * @param P The type of the parameters associated with each page of data.
     * @param D The type of the data items.
     * @param E The type of custom errors that can occur during loading.
     */
    sealed class LoadResult<Id : Comparable<Id>, out K : Any, out P : Any, out D : Any, out E : Any> {
        sealed class Error<Id : Comparable<Id>, K : Any, P : Any, D : Any, E : Any> : LoadResult<Id, K, P, D, E>() {
            data class Exception<Id : Comparable<Id>, K : Any, P : Any, D : Any, E : Any>(val error: Throwable) : Error<Id, K, P, D, E>()
            data class Custom<Id : Comparable<Id>, K : Any, P : Any, D : Any, E : Any>(val error: E) : Error<Id, K, P, D, E>()
        }

        data class Data<Id : Comparable<Id>, K : Any, P : Any, D : Any>(val collection: PagingData.Collection<Id, K, P, D>) : LoadResult<Id, K, P, D, Nothing>()
    }
}

/**
 * Represents a strategy for aggregating loaded pages of data into a single instance of [PagingItems].
 *
 * The [AggregatingStrategy] determines how the loaded pages of data should be combined and ordered to form a coherent list of [PagingData.Single] items.
 * It takes into account the anchor position, prefetch position, paging configuration, and the current state of the paging buffer.
 *
 * @param Id The type of the unique identifier for each item in the paged data.
 * @param K The type of the key used for paging.
 * @param P The type of the parameters associated with each page of data.
 * @param D The type of the data items.
 */
interface AggregatingStrategy<Id : Comparable<Id>, K : Any, P : Any, D : Any> {

    /**
     * Aggregates the loaded pages of data into a single instance of [PagingItems].
     *
     * @param anchorPosition The current anchor position in the paged data.
     * @param prefetchPosition The position to prefetch data from, or `null` if no prefetching is needed.
     * @param pagingConfig The configuration of the pager, including page size and prefetch distance.
     * @param pagingBuffer The current state of the paging buffer, containing the loaded data.
     * @return The aggregated list of [PagingItems] representing the combined and ordered paging data.
     */
    fun aggregate(
        anchorPosition: PagingKey<K, P>,
        prefetchPosition: PagingKey<K, P>?,
        pagingConfig: PagingConfig,
        pagingBuffer: PagingBuffer<Id, K, P, D>,
    ): PagingItems<Id, K, P, D>
}

/**
 * Represents a list of paging items.
 *
 * @param Id The type of the unique identifier for each item in the paged data.
 * @param K The type of the key used for paging.
 * @param P The type of the parameters associated with each page of data.
 * @param D The type of the data items.
 * @property data The list of [PagingData.Single] items representing the paging data.
 */
data class PagingItems<Id : Comparable<Id>, K : Any, P : Any, D : Any>(
    val data: List<PagingData.Single<Id, K, P, D>>
)

/**
 * Represents different strategies for handling errors during the paging process.
 */
sealed interface ErrorHandlingStrategy {
    /**
     * Ignores errors and continues with the previous state.
     */
    data object Ignore : ErrorHandlingStrategy

    /**
     * Passes the error to the UI layer for handling.
     */
    data object PassThrough : ErrorHandlingStrategy

    /**
     * Retries the last failed load operation.
     *
     * @property maxRetries The maximum number of retries before passing the error to the UI. Default is 3.
     */
    data class RetryLast(
        val maxRetries: Int = 3
    ) : ErrorHandlingStrategy
}

/**
 * Represents a reducer for handling [PagingAction.User.Custom] actions.
 *
 * @param Id The type of the unique identifier for each item in the paged data.
 * @param K The type of the key used for paging.
 * @param P The type of the parameters associated with each page of data.
 * @param D The type of the data items.
 * @param E The type of errors that can occur during the paging process.
 * @param A The type of custom actions that can be dispatched.
 */
interface UserCustomActionReducer<Id : Comparable<Id>, K : Any, P : Any, D : Any, E : Any, A : Any> {
    /**
     * Reduces the current [PagingState] based on the custom user action.
     *
     * @param action The custom user action to reduce.
     * @param state The current [PagingState] before applying the action.
     * @return The new [PagingState] after applying the custom user action.
     */
    fun reduce(
        action: User.Custom<Id, K, P, D, E, A>,
        state: PagingState<Id, K, P, D, E>
    ): PagingState<Id, K, P, D, E>
}

/**
 * Represents a manager for the queue of pages to be loaded.
 *
 * @param K The type of the key used for paging.
 * @param P The type of the parameters associated with each page of data.
 */
interface QueueManager<K : Any, P : Any> {
    /**
     * Enqueues a page key to be loaded.
     *
     * @param key The [PagingKey] representing the page to be loaded.
     */
    fun enqueue(key: PagingKey<K, P>)
}


/**
 * Represents a mutable version of [PagingBuffer] that allows adding and updating paging data.
 *
 * @param Id The type of the unique identifier for each item in the paged data.
 * @param K The type of the key used for paging.
 * @param P The type of the parameters associated with each page of data.
 * @param D The type of the data items.
 */
interface MutablePagingBuffer<Id : Comparable<Id>, K : Any, P : Any, D : Any> : PagingBuffer<Id, K, P, D> {
    /**
     * Puts the loaded page of data associated with the specified [PagingSource.LoadParams] into the buffer.
     *
     * @param params The [PagingSource.LoadParams] associated with the loaded page.
     * @param page The [PagingSource.LoadResult.Data] representing the loaded page of data.
     */
    fun put(params: PagingSource.LoadParams<K, P>, page: PagingSource.LoadResult.Data<Id, K, P, D>)
}


inline fun <Id : Comparable<Id>, K : Any, P : Any, D : Any, E : Any, A : Any> mutablePagingBufferOf(maxSize: Int): MutablePagingBuffer<Id, K, P, D> {
    return RealMutablePagingBuffer<Id, K, P, D, E, A>(maxSize)
}

/**
 * Represents different insertion strategies for adding new data to the paging buffer.
 */
enum class InsertionStrategy {
    /**
     * Appends new data to the end of the buffer.
     */
    APPEND,

    /**
     * Prepends new data to the beginning of the buffer.
     */
    PREPEND,

    /**
     * Replaces the existing data in the buffer with the new data.
     */
    REPLACE,
}

/**
 * Represents a collector for [PagingSource.LoadResult] objects.
 *
 * @param Id The type of the unique identifier for each item in the paged data.
 * @param K The type of the key used for paging.
 * @param P The type of the parameters associated with each page of data.
 * @param D The type of the data items.
 * @param E The type of errors that can occur during the paging process.
 * @param A The type of custom actions that can be dispatched.
 */
interface PagingSourceCollector<Id : Comparable<Id>, K : Any, P : Any, D : Any, E : Any, A : Any> {
    /**
     * Collects the load results from the [PagingSource] and dispatches appropriate [PagingAction] objects.
     *
     * @param params The [PagingSource.LoadParams] associated with the load operation.
     * @param results The flow of [PagingSource.LoadResult] instances representing the load results.
     * @param state The current [PagingState] when collecting the load results.
     * @param dispatch The function to dispatch [PagingAction] instances based on the load results.
     */
    suspend operator fun invoke(
        params: PagingSource.LoadParams<K, P>,
        results: Flow<PagingSource.LoadResult<Id, K, P, D, E>>,
        state: PagingState<Id, K, P, D, E>,
        dispatch: (action: PagingAction<Id, K, P, D, E, A>) -> Unit
    )
}

/**
 * Represents a provider of [PagingSource.LoadResult] streams.
 *
 * @param Id The type of the unique identifier for each item in the paged data.
 * @param K The type of the key used for paging.
 * @param P The type of the parameters associated with each page of data.
 * @param D The type of the data items.
 * @param E The type of errors that can occur during the paging process.
 */
interface PagingSourceStreamProvider<Id : Comparable<Id>, K : Any, P : Any, D : Any, E : Any> {
    /**
     * Provides a flow of [PagingSource.LoadResult] instances for the specified [PagingSource.LoadParams].
     *
     * @param params The [PagingSource.LoadParams] for which to provide the load result stream.
     * @return A flow of [PagingSource.LoadResult] instances representing the load results.
     */
    fun provide(params: PagingSource.LoadParams<K, P>): Flow<PagingSource.LoadResult<Id, K, P, D, E>>
}


/**
 * A type alias for an [Effect] that loads the next page of data when the paging state is [PagingState.Data.Idle].
 *
 * @param Id The type of the unique identifier for each item in the paged data.
 * @param K The type of the key used for paging.
 * @param P The type of the parameters associated with each page of data.
 * @param D The type of the data items.
 * @param E The type of errors that can occur during the paging process.
 * @param A The type of custom actions that can be dispatched.
 */
typealias LoadNextEffect<Id, K, P, D, E, A> = Effect<Id, K, P, D, E, A, UpdateData<Id, K, P, D, E, A>, Data.Idle<Id, K, P, D, E>>

typealias AppLoadEffect<Id, K, P, D, E, A> = Effect<Id, K, P, D, E, A, Load<Id, K, P, D, E, A>, PagingState<Id, K, P, D, E>>
typealias UserLoadEffect<Id, K, P, D, E, A> = Effect<Id, K, P, D, E, A, User.Load<Id, K, P, D, E, A>, Loading<Id, K, P, D, E>>
typealias UserLoadMoreEffect<Id, K, P, D, E, A> = Effect<Id, K, P, D, E, A, User.Load<Id, K, P, D, E, A>, Data.LoadingMore<Id, K, P, D, E>>