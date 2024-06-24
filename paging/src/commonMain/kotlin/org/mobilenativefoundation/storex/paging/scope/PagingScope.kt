@file:OptIn(InternalSerializationApi::class)

package org.mobilenativefoundation.storex.paging.scope

import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.InternalSerializationApi
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.FetcherResult
import org.mobilenativefoundation.store.store5.Updater
import org.mobilenativefoundation.storex.paging.*
import org.mobilenativefoundation.storex.paging.custom.*
import org.mobilenativefoundation.storex.paging.db.DriverFactory
import org.mobilenativefoundation.storex.paging.internal.api.*
import org.mobilenativefoundation.storex.paging.internal.impl.DefaultFetchingStrategy
import org.mobilenativefoundation.storex.paging.internal.impl.KClassRegistry
import org.mobilenativefoundation.storex.paging.internal.impl.RealFetchingStateHolder
import org.mobilenativefoundation.storex.paging.internal.impl.store.*

interface PagingScope<Id : Identifier<Id>, K : Comparable<K>, V : Identifiable<Id>> {
    val pager: PagerV2<Id>
    val operator: Operator<Id, K, V>
    val updatingItemProvider: UpdatingItemProvider<Id, V>
    val dispatcher: Dispatcher<K>
    val database: Database<Id, K, V>?

    class Builder<Id : Identifier<Id>, K : Comparable<K>, V : Identifiable<Id>>(
        private val pagingConfig: PagingConfig<Id, K>,
        private val database: Database<Id, K, V>?
    ) {

        private var initialLoadParams = PagingSource.LoadParams(
            pagingConfig.initialKey,
            strategy = LoadStrategy.SkipCache,
            direction = LoadDirection.Append
        )

        private var initialState: PagingState<Id> = PagingState.initial()

        private val requestsFlow: MutableSharedFlow<PagingRequest<K>> = MutableSharedFlow(replay = 20)


        private var storexPagingSource: PagingSource<Id, K, V>? = null
        private var androidxPagingSource: androidx.paging.PagingSource<K, V>? = null

        private var coroutineDispatcher: CoroutineDispatcher = DispatcherProvider.io
        private var launchEffects: List<LaunchEffect> = emptyList()
        private var sideEffects: List<SideEffect<Id, V>> = emptyList()
        private var errorHandlingStrategy: ErrorHandlingStrategy = ErrorHandlingStrategy.RetryLast()
        private var middleware: List<Middleware<K>> = emptyList()
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

        private var operations: MutableList<Operation<Id, K, V>> = mutableListOf()

        private var recompositionMode: RecompositionMode = RecompositionMode.ContextClock

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

        fun recompositionMode(recompositionMode: RecompositionMode) = apply {
            this.recompositionMode = recompositionMode
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


        fun build(): PagingScope<Id, K, V> {

            val pageFetcher = storexPagingSource?.let { createFetcherFromStorexPagingSource(it) }
                ?: androidxPagingSource?.let { createFetcherFromAndroidxPagingSource(it) }
                ?: throw IllegalArgumentException("You must provide a paging source, either from storex or androidx!")

            val fetchingStateHolder = this.fetchingStateHolder ?: RealFetchingStateHolder(
                initialFetchingState
            )


            val linkedHashMap = LinkedHashMapManager(
                pageMemoryCache = pageMemoryCache,
                itemMemoryCache = itemMemoryCache,
                pagingConfig = pagingConfig,
                db = database,
            )

            val selfUpdatingItemPresenter = SelfUpdatingItemPresenter(
                itemMemoryCache = itemMemoryCache,
                fetchingStateHolder = fetchingStateHolder,
                updater = itemUpdater,
                linkedHashMap = linkedHashMap,
                itemFetcher = itemFetcher,
                db = database
            )

            val concurrentNormalizedStore = ConcurrentNormalizedStore(
                pageFetcher = pageFetcher,
                db = database,
                fetchingStateHolder = fetchingStateHolder,
                sideEffects = sideEffects,
                pagingConfig = pagingConfig,
                selfUpdatingItemPresenter,
                linkedHashMap
            )

            val requests = requestsFlow.asSharedFlow()


            val pager = RealPagerV2(
                coroutineDispatcher = coroutineDispatcher,
                fetchingStateHolder = fetchingStateHolder,
                launchEffects = launchEffects,
                errorHandlingStrategy = errorHandlingStrategy,
                middleware = middleware,
                fetchingStrategy = fetchingStrategy,
                initialLoadParams = initialLoadParams,
                concurrentNormalizedStore = concurrentNormalizedStore,
                initialState = initialState,
                requests = requests,
                recompositionMode = recompositionMode
            )

            val operator = RealOperator(operations)

            val updatingItemProvider: UpdatingItemProvider<Id, V> = RealUpdatingItemProvider()

            val dispatcher = RealDispatcher(requestsFlow)

            return RealPagingScope(
                pager,
                operator,
                updatingItemProvider,
                dispatcher,
                database,
            )
        }


        companion object {
            inline operator fun <reified Id : Identifier<Id>, reified K : Comparable<K>, reified V : Identifiable<Id>> invoke(
                pagingConfig: PagingConfig<Id, K>,
                driverFactory: DriverFactory,
            ): Builder<Id, K, V> {
                val registry = KClassRegistry(
                    id = Id::class,
                    key = K::class,
                    value = V::class
                )

                val database = RealDatabase(driverFactory, registry)

                return Builder(
                    pagingConfig,
                    database
                )
            }

            operator fun <Id : Identifier<Id>, K : Comparable<K>, V : Identifiable<Id>> invoke(pagingConfig: PagingConfig<Id, K>): Builder<Id, K, V> {
                return Builder(pagingConfig, null)
            }
        }

    }
}


class RealPagingScope<Id : Identifier<Id>, K : Comparable<K>, V : Identifiable<Id>>(
    override val pager: PagerV2<Id>,
    override val operator: Operator<Id, K, V>,
    override val updatingItemProvider: UpdatingItemProvider<Id, V>,
    override val dispatcher: Dispatcher<K>,
    override val database: Database<Id, K, V>?
) : PagingScope<Id, K, V>