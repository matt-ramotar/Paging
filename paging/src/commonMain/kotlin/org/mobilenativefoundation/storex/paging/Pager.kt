@file:OptIn(InternalSerializationApi::class)

package org.mobilenativefoundation.storex.paging

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.FetcherResult
import org.mobilenativefoundation.storex.paging.custom.*
import org.mobilenativefoundation.storex.paging.db.DriverFactory
import org.mobilenativefoundation.storex.paging.internal.api.DispatcherProvider
import org.mobilenativefoundation.storex.paging.internal.api.FetchingState
import org.mobilenativefoundation.storex.paging.internal.impl.*
import org.mobilenativefoundation.storex.paging.internal.impl.DefaultErrorFactory
import kotlin.reflect.KClass

interface SelfUpdatingItemFactory<Id : Comparable<Id>, Q : Quantifiable<Id>, V : Identifiable<Id, Q>, E : Any> {
    fun createSelfUpdatingItem(id: Q): SelfUpdatingItem<Id, Q, V, E>
}


interface Pager<Id : Comparable<Id>, Q : Quantifiable<Id>, K : Any, V : Identifiable<Id, Q>, E : Any> :
    SelfUpdatingItemFactory<Id, Q, V, E> {

    // TODO(): Design decision to support incremental/decremental loading as well as manually force fetching by explicitly providing params

//    @Composable
//    fun pagingState(requests: Flow<PagingRequest<K>>): PagingState<Id, Q,E>

    // TODO(): This is good for testing, but from Compose UI - I want a state flow
    fun pagingFlow(
        requests: Flow<PagingRequest<K>>,
        recompositionMode: RecompositionMode = RecompositionMode.ContextClock
    ): Flow<PagingState<Id, Q, E>>

    fun pagingState(
        requests: Flow<PagingRequest<K>>
    ): StateFlow<PagingState<Id, Q, E>>

    @OptIn(InternalSerializationApi::class)
    class Builder<Id : Comparable<Id>, Q : Quantifiable<Id>, K : Comparable<K>, V : Identifiable<Id, Q>, E : Any, P : Any>(
        private val idKClass: KClass<Id>,
        private val keyKClass: KClass<K>,
        private val valueKClass: KClass<V>,
        private val errorKClass: KClass<E>,
        private val qKClass: KClass<Q>,
        private val pagingConfig: PagingConfig<Id, Q, K>,
        private val driverFactory: DriverFactory?,
        private val errorFactory: ErrorFactory<E>,
        private val operations: List<Operation<Id, Q, K, V, P, P>>
    ) {
        private var storexPagingSource: PagingSource<Id, Q, K, V, E>? = null
        private var androidxPagingSource: androidx.paging.PagingSource<K, V>? = null

        private var coroutineDispatcher: CoroutineDispatcher = DispatcherProvider.io
        private var launchEffects: List<LaunchEffect> = emptyList()
        private var sideEffects: List<SideEffect<Id, Q, V>> = emptyList()
        private var errorHandlingStrategy: ErrorHandlingStrategy = ErrorHandlingStrategy.RetryLast()
        private var middleware: List<Middleware<K>> = emptyList()
        private var initialState: PagingState<Id, Q, E> = PagingState.initial()
        private var initialFetchingState: FetchingState<Id, K> = FetchingState()
        private var itemFetcher: Fetcher<Id, V>? = null
        private var fetchingStrategy: FetchingStrategy<Id, Q, K, E> =
            DefaultFetchingStrategy(pagingConfig)

        fun coroutineDispatcher(coroutineDispatcher: CoroutineDispatcher) = apply {
            this.coroutineDispatcher = coroutineDispatcher
        }

        fun launchEffects(launchEffects: List<LaunchEffect>) = apply {
            this.launchEffects = launchEffects
        }

        fun sideEffects(sideEffects: List<SideEffect<Id, Q, V>>) = apply {
            this.sideEffects = sideEffects
        }

        fun errorHandlingStrategy(errorHandlingStrategy: ErrorHandlingStrategy) = apply {
            this.errorHandlingStrategy = errorHandlingStrategy
        }

        fun middleware(middleware: List<Middleware<K>>) = apply {
            this.middleware = middleware
        }

        fun fetchingStrategy(fetchingStrategy: FetchingStrategy<Id, Q, K, E>) = apply {
            this.fetchingStrategy = fetchingStrategy
        }

        fun initialState(initialState: PagingState<Id, Q, E>) = apply {
            this.initialState = initialState
        }

        fun initialFetchingState(initialFetchingState: FetchingState<Id, K>) = apply {
            this.initialFetchingState = initialFetchingState
        }

        fun storexPagingSource(storexPagingSource: PagingSource<Id, Q, K, V, E>) = apply {
            this.storexPagingSource = storexPagingSource
        }

        fun androidxPagingSource(androidxPagingSource: androidx.paging.PagingSource<K, V>) = apply {
            this.androidxPagingSource = androidxPagingSource
        }

        private fun createFetcherFromAndroidxPagingSource(androidxPagingSource: androidx.paging.PagingSource<K, V>): Fetcher<PagingSource.LoadParams<K>, PagingSource.LoadResult.Data<Id, Q, K, V, E>> {
            return Fetcher.ofResult {
                val androidxLoadParams =
                    it.androidx(pagingConfig.pageSize, pagingConfig.placeholderId != null)

                when (val androidxLoadResult = androidxPagingSource.load(androidxLoadParams)) {
                    is androidx.paging.PagingSource.LoadResult.Error -> {
                        FetcherResult.Error.Exception(
                            PagingError(
                                encodedError = Json.encodeToString(
                                    errorKClass.serializer(),
                                    errorFactory.create(androidxLoadResult.throwable)
                                ),
                                extras = androidxLoadResult.throwable.extras()
                            )
                        )
                    }

                    is androidx.paging.PagingSource.LoadResult.Invalid -> {
                        FetcherResult.Error.Exception(
                            PagingError(
                                encodedError = Json.encodeToString(
                                    errorKClass.serializer(),
                                    errorFactory.create(Throwable("Invalid")) // TODO() use types
                                ),
                                extras = null
                            )
                        )
                    }

                    is androidx.paging.PagingSource.LoadResult.Page -> {
                        FetcherResult.Data(androidxLoadResult.storex(it))
                    }
                }
            }
        }

        private fun createFetcherFromStorexPagingSource(storexPagingSource: PagingSource<Id, Q, K, V, E>): Fetcher<PagingSource.LoadParams<K>, PagingSource.LoadResult.Data<Id, Q, K, V, E>> {
            println("IN FETCHER")
            return Fetcher.ofResult {
                println("IN FETCHER FLOW WITH $it")
                when (val loadResult = storexPagingSource.load(it)) {
                    is PagingSource.LoadResult.Data -> {
                        println("DATA LOAD RESULT = $loadResult")
                        FetcherResult.Data(loadResult)
                    }

                    is PagingSource.LoadResult.Error -> {
                        println("ERROR LOAD RESULT = $loadResult")
                        FetcherResult.Error.Exception(
                            PagingError(
                                encodedError = Json.encodeToString(
                                    errorKClass.serializer(),
                                    loadResult.error
                                ),
                                extras = loadResult.extras
                            )
                        )
                    }
                }
            }

        }


        fun build(): Pager<Id, Q, K, V, E> {

            val registry = KClassRegistry(
                id = idKClass,
                q = qKClass,
                key = keyKClass,
                value = valueKClass,
                error = errorKClass,
            )

            val pageFetcher = storexPagingSource?.let { createFetcherFromStorexPagingSource(it) }
                ?: androidxPagingSource?.let { createFetcherFromAndroidxPagingSource(it) }
                ?: throw IllegalArgumentException("You must provide a paging source, either from storex or androidx!")

            val fetchingStateHolder = RealFetchingStateHolder(
                initialFetchingState
            )


            val normalizedStore = RealNormalizedStore(
                pageFetcher = pageFetcher,
                registry = registry,
                errorFactory = errorFactory,
                itemFetcher = itemFetcher,
                driverFactory = driverFactory,
                maxSize = pagingConfig.maxSize,
                fetchingStateHolder = fetchingStateHolder,
                sideEffects = sideEffects,
                pagingConfig = pagingConfig,
            )

            return RealPager(
                coroutineDispatcher = coroutineDispatcher,
                fetchingStateHolder = fetchingStateHolder,
                launchEffects = launchEffects,
                errorHandlingStrategy = errorHandlingStrategy,
                middleware = middleware,
                fetchingStrategy = fetchingStrategy,
                initialLoadParams = PagingSource.LoadParams(
                    pagingConfig.initialKey,
                    strategy = LoadStrategy.SkipCache,
                    direction = LoadDirection.Append
                ),
                registry = registry,
                normalizedStore = normalizedStore,
                operations = operations,
                initialState = initialState,
            )
        }

        companion object {
            inline operator fun <reified Id : Comparable<Id>, reified Q : Quantifiable<Id>, reified K : Comparable<K>, reified V : Identifiable<Id, Q>, reified E : Any, P : Any> invoke(
                pagingConfig: PagingConfig<Id, Q, K>,
                driverFactory: DriverFactory,
                errorFactory: ErrorFactory<E>,
                operations: List<Operation<Id, Q, K, V, P, P>>,
            ): Builder<Id, Q, K, V, E, P> {
                return Builder(
                    idKClass = Id::class,
                    keyKClass = K::class,
                    qKClass = Q::class,
                    valueKClass = V::class,
                    errorKClass = E::class,
                    pagingConfig = pagingConfig,
                    driverFactory = driverFactory,
                    errorFactory = errorFactory,
                    operations = operations
                )
            }

            inline operator fun <reified Id : Comparable<Id>, reified Q : Quantifiable<Id>, reified K : Comparable<K>, reified V : Identifiable<Id, Q>, reified E : Any> invoke(
                pagingConfig: PagingConfig<Id, Q, K>,
                driverFactory: DriverFactory,
                errorFactory: ErrorFactory<E>,
            ): Builder<Id, Q, K, V, E, Any> {
                return Builder(
                    idKClass = Id::class,
                    keyKClass = K::class,
                    qKClass = Q::class,
                    valueKClass = V::class,
                    errorKClass = E::class,
                    pagingConfig = pagingConfig,
                    driverFactory = driverFactory,
                    errorFactory = errorFactory,
                    operations = emptyList()
                )
            }

            inline operator fun <reified Id : Comparable<Id>, reified Q : Quantifiable<Id>, reified K : Comparable<K>, reified V : Identifiable<Id, Q>> invoke(
                pagingConfig: PagingConfig<Id, Q, K>,
                driverFactory: DriverFactory,
            ): Builder<Id, Q, K, V, Throwable, Any> {
                return Builder(
                    idKClass = Id::class,
                    keyKClass = K::class,
                    valueKClass = V::class,
                    qKClass = Q::class,
                    errorKClass = Throwable::class,
                    pagingConfig = pagingConfig,
                    driverFactory = driverFactory,
                    errorFactory = DefaultErrorFactory(),
                    operations = emptyList()
                )
            }

            inline operator fun <reified Id : Comparable<Id>, reified Q : Quantifiable<Id>, reified K : Comparable<K>, reified V : Identifiable<Id, Q>> invoke(
                pagingConfig: PagingConfig<Id, Q, K>,
            ): Builder<Id, Q, K, V, Throwable, Any> {
                return Builder(
                    idKClass = Id::class,
                    keyKClass = K::class,
                    valueKClass = V::class,
                    qKClass = Q::class,
                    errorKClass = Throwable::class,
                    pagingConfig = pagingConfig,
                    driverFactory = null,
                    errorFactory = DefaultErrorFactory(),
                    operations = emptyList()
                )
            }
        }
    }
}


sealed class PagingRequest<out K : Any> {

    data class ProcessQueue internal constructor(
        val direction: LoadDirection
    ) : PagingRequest<Nothing>()

    data class SkipQueue<K : Any> internal constructor(
        val key: K,
        val direction: LoadDirection,
        val strategy: LoadStrategy,
    ) : PagingRequest<K>()

    data class Enqueue<K : Any> internal constructor(
        val key: K,
        val direction: LoadDirection,
        val strategy: LoadStrategy,
        val jump: Boolean
    ) : PagingRequest<K>()

    data object Invalidate : PagingRequest<Nothing>()

    companion object {
        fun <K : Any> skipQueue(
            key: K,
            direction: LoadDirection,
            strategy: LoadStrategy = LoadStrategy.SkipCache
        ) =
            SkipQueue(key, direction, strategy)

        fun processQueue(direction: LoadDirection) = ProcessQueue(direction)
        fun <K : Any> enqueue(key: K, jump: Boolean, strategy: LoadStrategy = LoadStrategy.SkipCache) =
            Enqueue(key, LoadDirection.Append, strategy, jump)

        fun invalidate() = Invalidate
    }
}