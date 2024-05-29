@file:OptIn(InternalSerializationApi::class)

package org.mobilenativefoundation.storex.paging

import androidx.compose.runtime.Composable
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.FetcherResult
import org.mobilenativefoundation.storex.paging.custom.ErrorFactory
import org.mobilenativefoundation.storex.paging.custom.ErrorHandlingStrategy
import org.mobilenativefoundation.storex.paging.custom.FetchingStrategy
import org.mobilenativefoundation.storex.paging.custom.LaunchEffect
import org.mobilenativefoundation.storex.paging.custom.Middleware
import org.mobilenativefoundation.storex.paging.custom.Operation
import org.mobilenativefoundation.storex.paging.custom.SideEffect
import org.mobilenativefoundation.storex.paging.db.DriverFactory
import org.mobilenativefoundation.storex.paging.internal.api.DispatcherProvider
import org.mobilenativefoundation.storex.paging.internal.api.FetchingState
import org.mobilenativefoundation.storex.paging.internal.impl.DefaultErrorFactory
import org.mobilenativefoundation.storex.paging.internal.impl.DefaultFetchingStrategy
import org.mobilenativefoundation.storex.paging.internal.impl.KClassRegistry
import org.mobilenativefoundation.storex.paging.internal.impl.PagingError
import org.mobilenativefoundation.storex.paging.internal.impl.RealFetchingStateHolder
import org.mobilenativefoundation.storex.paging.internal.impl.RealNormalizedStore
import org.mobilenativefoundation.storex.paging.internal.impl.RealPager
import org.mobilenativefoundation.storex.paging.internal.impl.extras
import kotlin.reflect.KClass


interface Pager<Id : Comparable<Id>, K : Any, V : Identifiable<Id>, E : Any> {

    @Composable
    fun pagingState(loadParams: Flow<PagingSource.LoadParams<K>>): PagingState<Id, E>

    fun selfUpdatingItem(id: Quantifiable<Id>): SelfUpdatingItem<Id, V, E>


    @OptIn(InternalSerializationApi::class)
    class Builder<Id : Comparable<Id>, K : Any, V : Identifiable<Id>, E : Any, P : Any>(
        private val idKClass: KClass<Id>,
        private val keyKClass: KClass<K>,
        private val valueKClass: KClass<V>,
        private val errorKClass: KClass<E>,
        private val pagingConfig: PagingConfig<Id, K>,
        private val driverFactory: DriverFactory?,
        private val errorFactory: ErrorFactory<E>,
        private val operations: List<Operation<Id, K, V, P, P>>
    ) {
        private var storexPagingSource: PagingSource<Id, K, V, E>? = null
        private var androidxPagingSource: androidx.paging.PagingSource<K, V>? = null

        private var coroutineDispatcher: CoroutineDispatcher = DispatcherProvider.io
        private var launchEffects: List<LaunchEffect> = emptyList()
        private var sideEffects: List<SideEffect<Id, V>> = emptyList()
        private var errorHandlingStrategy: ErrorHandlingStrategy = ErrorHandlingStrategy.RetryLast()
        private var middleware: List<Middleware<K>> = emptyList()
        private var initialState: PagingState<Id, E> = PagingState.initial()
        private var initialFetchingState: FetchingState<Id> = FetchingState()
        private var itemFetcher: Fetcher<Id, V>? = null
        private var fetchingStrategy: FetchingStrategy<Id, K, E> =
            DefaultFetchingStrategy(pagingConfig)

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

        fun fetchingStrategy(fetchingStrategy: FetchingStrategy<Id, K, E>) = apply {
            this.fetchingStrategy = fetchingStrategy
        }

        fun initialState(initialState: PagingState<Id, E>) = apply {
            this.initialState = initialState
        }

        fun initialFetchingState(initialFetchingState: FetchingState<Id>) = apply {
            this.initialFetchingState = initialFetchingState
        }

        fun storexPagingSource(storexPagingSource: PagingSource<Id, K, V, E>) = apply {
            this.storexPagingSource = storexPagingSource
        }

        fun androidxPagingSource(storexPagingSource: androidx.paging.PagingSource<K, V>) = apply {
            this.androidxPagingSource = storexPagingSource
        }

        private fun createFetcherFromAndroidxPagingSource(androidxPagingSource: androidx.paging.PagingSource<K, V>): Fetcher<PagingSource.LoadParams<K>, PagingSource.LoadResult.Data<Id, K, V, E>> {
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

        private fun createFetcherFromStorexPagingSource(storexPagingSource: PagingSource<Id, K, V, E>): Fetcher<PagingSource.LoadParams<K>, PagingSource.LoadResult.Data<Id, K, V, E>> {
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


        fun build(): Pager<Id, K, V, E> {

            val registry = KClassRegistry(
                id = idKClass,
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
                    strategy = PagingSource.LoadParams.Strategy.SkipCache,
                    direction = PagingSource.LoadParams.Direction.Append
                ),
                registry = registry,
                normalizedStore = normalizedStore,
                operations = operations,
                initialState = initialState,
            )
        }

        companion object {
            inline operator fun <reified Id : Comparable<Id>, reified K : Any, reified V : Identifiable<Id>, reified E : Any, P : Any> invoke(
                pagingConfig: PagingConfig<Id, K>,
                driverFactory: DriverFactory,
                errorFactory: ErrorFactory<E>,
                operations: List<Operation<Id, K, V, P, P>>,
            ): Builder<Id, K, V, E, P> {
                return Builder(
                    idKClass = Id::class,
                    keyKClass = K::class,
                    valueKClass = V::class,
                    errorKClass = E::class,
                    pagingConfig = pagingConfig,
                    driverFactory = driverFactory,
                    errorFactory = errorFactory,
                    operations = operations
                )
            }

            inline operator fun <reified Id : Comparable<Id>, reified K : Any, reified V : Identifiable<Id>, reified E : Any> invoke(
                pagingConfig: PagingConfig<Id, K>,
                driverFactory: DriverFactory,
                errorFactory: ErrorFactory<E>,
            ): Builder<Id, K, V, E, Any> {
                return Builder(
                    idKClass = Id::class,
                    keyKClass = K::class,
                    valueKClass = V::class,
                    errorKClass = E::class,
                    pagingConfig = pagingConfig,
                    driverFactory = driverFactory,
                    errorFactory = errorFactory,
                    operations = emptyList()
                )
            }

            inline operator fun <reified Id : Comparable<Id>, reified K : Any, reified V : Identifiable<Id>> invoke(
                pagingConfig: PagingConfig<Id, K>,
                driverFactory: DriverFactory,
            ): Builder<Id, K, V, Throwable, Any> {
                return Builder(
                    idKClass = Id::class,
                    keyKClass = K::class,
                    valueKClass = V::class,
                    errorKClass = Throwable::class,
                    pagingConfig = pagingConfig,
                    driverFactory = driverFactory,
                    errorFactory = DefaultErrorFactory(),
                    operations = emptyList()
                )
            }

            inline operator fun <reified Id : Comparable<Id>, reified K : Any, reified V : Identifiable<Id>> invoke(
                pagingConfig: PagingConfig<Id, K>,
            ): Builder<Id, K, V, Throwable, Any> {
                return Builder(
                    idKClass = Id::class,
                    keyKClass = K::class,
                    valueKClass = V::class,
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


