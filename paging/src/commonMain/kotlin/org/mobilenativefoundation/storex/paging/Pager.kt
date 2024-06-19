@file:OptIn(InternalSerializationApi::class)

package org.mobilenativefoundation.storex.paging

import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.InternalSerializationApi
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.FetcherResult
import org.mobilenativefoundation.store.store5.Updater
import org.mobilenativefoundation.storex.paging.custom.*
import org.mobilenativefoundation.storex.paging.db.DriverFactory
import org.mobilenativefoundation.storex.paging.internal.api.*
import org.mobilenativefoundation.storex.paging.internal.impl.*
import org.mobilenativefoundation.storex.paging.internal.impl.store.*
import kotlin.reflect.KClass

interface SelfUpdatingItemFactory<Id : Identifier<Id>, V : Identifiable<Id>> {
    fun createSelfUpdatingItem(id: Id): SelfUpdatingItem<Id, V>
}


interface Pager<Id : Identifier<Id>, K : Comparable<K>, V : Identifiable<Id>> {

    // TODO(): Design decision to support incremental/decremental loading as well as manually force fetching by explicitly providing params

//    @Composable
//    fun pagingState(requests: Flow<PagingRequest<K>>): PagingState<Id, Q,E>

    // TODO(): This is good for testing, but from Compose UI - I want a state flow
    fun pagingFlow(
        requests: Flow<PagingRequest<K>>,
        recompositionMode: RecompositionMode = RecompositionMode.ContextClock
    ): Flow<PagingState<Id>>

    fun pagingStateFlow(
        coroutineScope: CoroutineScope,
        requests: Flow<PagingRequest<K>>
    ): StateFlow<PagingState<Id>>

    fun pagingItems(
        coroutineScope: CoroutineScope,
        requests: Flow<PagingRequest<K>>,
    ): StateFlow<List<V>>

    @OptIn(InternalSerializationApi::class)
    class Builder<Id : Identifier<Id>, K : Comparable<K>, V : Identifiable<Id>>(
        private val idKClass: KClass<Id>,
        private val keyKClass: KClass<K>,
        private val valueKClass: KClass<V>,
        private val pagingConfig: PagingConfig<Id, K>,
        private val driverFactory: DriverFactory?,
        private val operations: List<Operation<Id, K, V>>
    ) {
        private var storexPagingSource: PagingSource<Id, K, V>? = null
        private var androidxPagingSource: androidx.paging.PagingSource<K, V>? = null

        private var coroutineDispatcher: CoroutineDispatcher = DispatcherProvider.io
        private var launchEffects: List<LaunchEffect> = emptyList()
        private var sideEffects: List<SideEffect<Id, V>> = emptyList()
        private var errorHandlingStrategy: ErrorHandlingStrategy = ErrorHandlingStrategy.RetryLast()
        private var middleware: List<Middleware<K>> = emptyList()
        private var initialState: PagingState<Id> = PagingState.initial()
        private var initialFetchingState: FetchingState<Id, K> = FetchingState()
        private var itemFetcher: Fetcher<Id, V>? = null
        private var fetchingStrategy: FetchingStrategy<Id, K> =
            DefaultFetchingStrategy(pagingConfig)


        private var registry: KClassRegistry<Id, K, V>? = null

        private var fetchingStateHolder: FetchingStateHolder<Id, K>? = null

        private var normalizedStore: NormalizedStore<Id, K, V>? = null

        private var pagingOperationsManager: PagingOperationsManager<Id, K, V>? = null

        private var pageMemoryCache: PageMemoryCache<K, Id> = mutableMapOf()
        private var itemMemoryCache: ItemMemoryCache<Id, V> = mutableMapOf()
        private var itemUpdater: Updater<Id, V, *>? = null

        @VisibleForTesting
        fun operationManager(pagingOperationsManager: PagingOperationsManager<Id, K, V>) = apply {
            this.pagingOperationsManager = pagingOperationsManager
        }

        @VisibleForTesting
        fun registry(registry: KClassRegistry<Id, K, V>) = apply {
            this.registry = registry
        }

        @VisibleForTesting
        fun fetchingStateHolder(fetchingStateHolder: FetchingStateHolder<Id, K>) = apply {
            this.fetchingStateHolder = fetchingStateHolder
        }

        @VisibleForTesting
        fun normalizedStore(normalizedStore: NormalizedStore<Id, K, V>) = apply {
            this.normalizedStore = normalizedStore
        }

        fun itemMemoryCache(itemMemoryCache: ItemMemoryCache<Id, V>) = apply {
            this.itemMemoryCache = itemMemoryCache
        }

        fun pageMemoryCache(pageMemoryCache: PageMemoryCache<K, Id>) = apply {
            this.pageMemoryCache = pageMemoryCache
        }

        fun coroutineDispatcher(coroutineDispatcher: CoroutineDispatcher) = apply {
            this.coroutineDispatcher = coroutineDispatcher
        }

        fun launchEffects(launchEffects: List<LaunchEffect>) = apply {
            this.launchEffects = launchEffects
        }

        fun sideEffects(sideEffects: List<SideEffect<Id, V>>) = apply {
            this.sideEffects = sideEffects
        }

        fun errorHandlingStrategy(errorHandlingStrategy: ErrorHandlingStrategy) = apply {
            this.errorHandlingStrategy = errorHandlingStrategy
        }

        fun middleware(middleware: List<Middleware<K>>) = apply {
            this.middleware = middleware
        }

        fun fetchingStrategy(fetchingStrategy: FetchingStrategy<Id, K>) = apply {
            this.fetchingStrategy = fetchingStrategy
        }

        fun initialState(initialState: PagingState<Id>) = apply {
            this.initialState = initialState
        }

        fun initialFetchingState(initialFetchingState: FetchingState<Id, K>) = apply {
            this.initialFetchingState = initialFetchingState
        }

        fun storexPagingSource(storexPagingSource: PagingSource<Id, K, V>) = apply {
            this.storexPagingSource = storexPagingSource
        }

        fun androidxPagingSource(androidxPagingSource: androidx.paging.PagingSource<K, V>) = apply {
            this.androidxPagingSource = androidxPagingSource
        }

        private fun createFetcherFromAndroidxPagingSource(androidxPagingSource: androidx.paging.PagingSource<K, V>): Fetcher<PagingSource.LoadParams<K>, PagingSource.LoadResult.Data<Id, K, V>> {
            return Fetcher.ofResult {
                val androidxLoadParams =
                    it.androidx(pagingConfig.pageSize, pagingConfig.placeholderId != null)

                when (val androidxLoadResult = androidxPagingSource.load(androidxLoadParams)) {
                    is androidx.paging.PagingSource.LoadResult.Error -> {
                        FetcherResult.Error.Exception(
                            androidxLoadResult.throwable
                        )
                    }

                    is androidx.paging.PagingSource.LoadResult.Invalid -> {
                        FetcherResult.Error.Exception(
                            Throwable(
                                "Invalid"
                            )
                        )
                    }

                    is androidx.paging.PagingSource.LoadResult.Page -> {
                        FetcherResult.Data(androidxLoadResult.storex(it))
                    }
                }
            }
        }

        private fun createFetcherFromStorexPagingSource(storexPagingSource: PagingSource<Id, K, V>): Fetcher<PagingSource.LoadParams<K>, PagingSource.LoadResult.Data<Id, K, V>> {
            println("IN FETCHER")
            return Fetcher.ofResult {
                println("IN FETCHER FLOW WITH $it")
                when (val loadResult = storexPagingSource.load(it)) {
                    is PagingSource.LoadResult.Data -> {
                        println("DATA LOAD RESULT = $loadResult")
                        FetcherResult.Data(loadResult)
                    }

                    is PagingSource.LoadResult.Error.Exception -> {
                        println("ERROR LOAD RESULT = $loadResult")
                        FetcherResult.Error.Exception(
                            loadResult.error
                        )
                    }

                    is PagingSource.LoadResult.Error.Message -> {
                        println("ERROR LOAD RESULT = $loadResult")
                        FetcherResult.Error.Exception(
                            Throwable(loadResult.error)
                        )
                    }
                }
            }

        }


        fun build(): Pager<Id, K, V> {


            val registry = this.registry ?: KClassRegistry(
                id = idKClass,
                key = keyKClass,
                value = valueKClass,
            )

            val pageFetcher = storexPagingSource?.let { createFetcherFromStorexPagingSource(it) }
                ?: androidxPagingSource?.let { createFetcherFromAndroidxPagingSource(it) }
                ?: throw IllegalArgumentException("You must provide a paging source, either from storex or androidx!")

            val fetchingStateHolder = this.fetchingStateHolder ?: RealFetchingStateHolder(
                initialFetchingState
            )

            val db = driverFactory?.createDriver()?.let { PagingDb(it) }

            val linkedHashMap = LinkedHashMapManager(
                pageMemoryCache = pageMemoryCache,
                itemMemoryCache = itemMemoryCache,
                registry = registry,
                pagingConfig = pagingConfig,
                db = db,
            )

            val selfUpdatingItemPresenter = SelfUpdatingItemPresenter(
                registry = registry,
                itemMemoryCache = itemMemoryCache,
                fetchingStateHolder = fetchingStateHolder,
                updater = itemUpdater,
                linkedHashMap = linkedHashMap,
                itemFetcher = itemFetcher,
                db = db
            )

            val concurrentNormalizedStore = ConcurrentNormalizedStore(
                pageFetcher = pageFetcher,
                registry = registry,
                db = db,
                fetchingStateHolder = fetchingStateHolder,
                sideEffects = sideEffects,
                pagingConfig = pagingConfig,
                selfUpdatingItemPresenter,
                linkedHashMap
            )

            val operationManager = this.pagingOperationsManager ?: RealPagingOperationsManager()

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
                concurrentNormalizedStore = concurrentNormalizedStore,
                initialState = initialState,
            )
        }

        companion object {
            inline operator fun <reified Id : Identifier<Id>, reified K : Comparable<K>, reified V : Identifiable<Id>> invoke(
                pagingConfig: PagingConfig<Id, K>,
                driverFactory: DriverFactory,
                operations: List<Operation<Id, K, V>>,
            ): Builder<Id, K, V> {
                return Builder(
                    idKClass = Id::class,
                    keyKClass = K::class,
                    valueKClass = V::class,
                    pagingConfig = pagingConfig,
                    driverFactory = driverFactory,
                    operations = operations
                )
            }

            inline operator fun <reified Id : Identifier<Id>, reified K : Comparable<K>, reified V : Identifiable<Id>> invoke(
                pagingConfig: PagingConfig<Id, K>,
                driverFactory: DriverFactory,
            ): Builder<Id, K, V> {
                return Builder(
                    idKClass = Id::class,
                    keyKClass = K::class,
                    valueKClass = V::class,
                    pagingConfig = pagingConfig,
                    driverFactory = driverFactory,
                    operations = emptyList()
                )
            }


            inline operator fun <reified Id : Identifier<Id>, reified K : Comparable<K>, reified V : Identifiable<Id>> invoke(
                pagingConfig: PagingConfig<Id, K>,
            ): Builder<Id, K, V> {
                return Builder(
                    idKClass = Id::class,
                    keyKClass = K::class,
                    valueKClass = V::class,
                    pagingConfig = pagingConfig,
                    driverFactory = null,
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