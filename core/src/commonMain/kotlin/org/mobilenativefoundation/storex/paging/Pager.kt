@file:OptIn(InternalSerializationApi::class)

package org.mobilenativefoundation.storex.paging

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.mobilenativefoundation.store.cache5.CacheBuilder
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.FetcherResult
import org.mobilenativefoundation.store5.cache.RealNormalizedCache
import org.mobilenativefoundation.store5.core.Identifiable
import org.mobilenativefoundation.storex.paging.custom.ErrorFactory
import org.mobilenativefoundation.storex.paging.custom.ErrorHandlingStrategy
import org.mobilenativefoundation.storex.paging.custom.FetchingStrategy
import org.mobilenativefoundation.storex.paging.custom.LaunchEffect
import org.mobilenativefoundation.storex.paging.custom.Middleware
import org.mobilenativefoundation.storex.paging.custom.SideEffect
import org.mobilenativefoundation.storex.paging.custom.TransformationStrategy
import org.mobilenativefoundation.storex.paging.db.DriverFactory
import org.mobilenativefoundation.storex.paging.internal.api.FetchingState
import org.mobilenativefoundation.storex.paging.internal.api.MutablePagingBuffer
import org.mobilenativefoundation.storex.paging.internal.impl.ItemStoreFactory
import org.mobilenativefoundation.storex.paging.internal.impl.KClassRegistry
import org.mobilenativefoundation.storex.paging.internal.impl.PageStoreFactory
import org.mobilenativefoundation.storex.paging.internal.impl.PagingError
import org.mobilenativefoundation.storex.paging.internal.impl.RealFetchingStateHolder
import org.mobilenativefoundation.storex.paging.internal.impl.RealMutablePagingBuffer
import org.mobilenativefoundation.storex.paging.internal.impl.RealPager
import kotlin.reflect.KClass


@Composable
fun <Id : Comparable<Id>, K : Any, V : Identifiable<Id>, E : Any> rememberUpdatingItem(
    pager: Pager<Id, K, V, E>,
    id: Id
): SelfUpdatingItem<Id, V, E> {
    val updatingItem = remember(id) {
        pager.selfUpdatingItem(id)
    }

    return updatingItem
}

interface Pager<Id : Comparable<Id>, K : Any, V : Identifiable<Id>, E : Any> {

    @Composable
    fun pagingState(loadParams: StateFlow<PagingSource.LoadParams<K>>): PagingState<Id, E>

    fun selfUpdatingItem(id: Id): SelfUpdatingItem<Id, V, E>


    class Builder<Id : Comparable<Id>, K : Any, V : Identifiable<Id>, E : Any, P : Any>(
        private val idKClass: KClass<Id>,
        private val keyKClass: KClass<K>,
        private val valueKClass: KClass<V>,
        private val errorKClass: KClass<E>
    ) {

        private lateinit var coroutineDispatcher: CoroutineDispatcher
        private lateinit var transformationParams: P
        private var transformations: List<TransformationStrategy<Id, V, P>> = emptyList()
        private var launchEffects: List<LaunchEffect> = emptyList()
        private var sideEffects: List<SideEffect<Id, V>> = emptyList()
        private var pagingBufferMaxSize: Int = 500
        private lateinit var errorHandlingStrategy: ErrorHandlingStrategy
        private lateinit var pagingSource: PagingSource<Id, K, V, E>
        private lateinit var middleware: List<Middleware<K>>
        private lateinit var fetchingStrategy: FetchingStrategy<Id, K, E>
        private lateinit var pagingConfig: PagingConfig<Id>
        private lateinit var initialLoadParams: PagingSource.LoadParams<K>
        private var initialState: PagingState<Id, E> = PagingState.initial()
        private var initialFetchingState: FetchingState<Id> = FetchingState()

        private lateinit var itemFetcher: Fetcher<Id, V>
        private lateinit var driverFactory: DriverFactory
        private lateinit var errorFactory: ErrorFactory<E>

        fun coroutineDispatcher(coroutineDispatcher: CoroutineDispatcher) = apply {
            this.coroutineDispatcher = coroutineDispatcher
        }

        fun transformationParams(transformationParams: P) = apply {
            this.transformationParams = transformationParams
        }

        fun transformations(transformations: List<TransformationStrategy<Id, V, P>>) = apply {
            this.transformations = transformations
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

        fun pagingConfig(pagingConfig: PagingConfig<Id>) = apply {
            this.pagingConfig = pagingConfig
        }

        fun initialLoadParams(initialLoadParams: PagingSource.LoadParams<K>) = apply {
            this.initialLoadParams = initialLoadParams
        }

        fun initialState(initialState: PagingState<Id, E>) = apply {
            this.initialState = initialState
        }

        fun initialFetchingState(initialFetchingState: FetchingState<Id>) = apply {
            this.initialFetchingState = initialFetchingState
        }

        fun pagingBufferMaxSize(maxSize: Int) = apply {
            this.pagingBufferMaxSize = maxSize
        }


        fun build(): Pager<Id, K, V, E> {

            val registry = KClassRegistry<Id, K, V, E>(
                id = idKClass,
                key = keyKClass,
                value = valueKClass,
                error = errorKClass,
            )

            val delegateItemCache = CacheBuilder<Id, V>().build()
            val normalizedCache = RealNormalizedCache<Id, K, V>(delegateItemCache)

            val pageFetcher =
                Fetcher.ofResult<PagingSource.LoadParams<K>, PagingSource.LoadResult.Data<Id, K, V, E>> {
                    when (val loadResult = pagingSource.load(it)) {
                        is PagingSource.LoadResult.Data -> FetcherResult.Data(loadResult)
                        is PagingSource.LoadResult.Error -> {
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


            val pageStoreFactory = PageStoreFactory<Id, K, V, E>(
                fetcher = pageFetcher,
                driverFactory = driverFactory,
                registry = registry,
                normalizedCache = normalizedCache
            )

            val itemStoreFactory = ItemStoreFactory<Id, K, V>(
                fetcher = itemFetcher,
                driverFactory = driverFactory,
                registry = registry,
                normalizedCache = normalizedCache
            )

            val pageStore = pageStoreFactory.create()
            val itemStore = itemStoreFactory.create()


            val fetchingStateHolder = RealFetchingStateHolder<Id>(
                initialFetchingState
            )

            val mutablePagingBuffer: MutablePagingBuffer<Id, K, V, E> =
                RealMutablePagingBuffer(
                    pagingBufferMaxSize,
                    pageStore,
                    itemStore,
                    transformationParams,
                    transformations
                )

            val normalizedStore = RealNormalizedStore(
                pagingSource,
                registry,
                errorFactory,
                itemFetcher,
                driverFactory,
                pagingBufferMaxSize,
                fetchingStateHolder,
                sideEffects,
                pagingConfig,
            )

            return RealPager<Id, K, V, E, P>(
                coroutineDispatcher,
                fetchingStateHolder,
                launchEffects,
                sideEffects,
                errorHandlingStrategy,
                middleware,
                fetchingStrategy,
                pagingConfig,
                initialLoadParams,
                errorFactory,
                registry,
                normalizedStore,
                transformationParams, transformations,
                initialState,
            )
        }

        companion object {
            inline operator fun <reified Id : Comparable<Id>, reified K : Any, reified V : Identifiable<Id>, reified E : Any, P : Any> invoke(): Builder<Id, K, V, E, P> {
                return Builder(Id::class, K::class, V::class, E::class)
            }
        }
    }
}