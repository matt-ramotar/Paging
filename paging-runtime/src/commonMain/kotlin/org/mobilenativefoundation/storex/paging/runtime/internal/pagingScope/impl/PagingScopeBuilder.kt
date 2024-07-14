package org.mobilenativefoundation.storex.paging.runtime.internal.pagingScope.impl

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableSharedFlow
import org.mobilenativefoundation.store.store5.Updater
import org.mobilenativefoundation.storex.paging.custom.FetchingStrategy
import org.mobilenativefoundation.storex.paging.custom.LaunchEffect
import org.mobilenativefoundation.storex.paging.custom.Middleware
import org.mobilenativefoundation.storex.paging.custom.SideEffect
import org.mobilenativefoundation.storex.paging.persistence.api.ItemPersistence
import org.mobilenativefoundation.storex.paging.persistence.api.PagePersistence
import org.mobilenativefoundation.storex.paging.persistence.impl.RealDataPersistence
import org.mobilenativefoundation.storex.paging.runtime.Action
import org.mobilenativefoundation.storex.paging.runtime.ErrorHandlingStrategy
import org.mobilenativefoundation.storex.paging.runtime.FetchingState
import org.mobilenativefoundation.storex.paging.runtime.Identifiable
import org.mobilenativefoundation.storex.paging.runtime.Identifier
import org.mobilenativefoundation.storex.paging.runtime.LoadDirection
import org.mobilenativefoundation.storex.paging.runtime.LoadStrategy
import org.mobilenativefoundation.storex.paging.runtime.PagingConfig
import org.mobilenativefoundation.storex.paging.runtime.PagingScope
import org.mobilenativefoundation.storex.paging.runtime.PagingSource
import org.mobilenativefoundation.storex.paging.runtime.PagingState
import org.mobilenativefoundation.storex.paging.runtime.PlaceholderFactory
import org.mobilenativefoundation.storex.paging.runtime.RecompositionMode
import org.mobilenativefoundation.storex.paging.runtime.internal.logger.impl.RealPagingLogger
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.api.FetchingStateHolder
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.api.LinkedHashMapManager
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.api.ListSortAnalyzer
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.api.LoadingHandler
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.api.OperationApplier
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.api.PagingStateManager
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.api.QueueManager
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.impl.ConcurrentFetchingStateHolder
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.impl.ConcurrentOperationApplier
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.impl.ConcurrentOperationManager
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.impl.DefaultFetchingStrategy
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.impl.DefaultListSortAnalyzer
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.impl.RealLinkedHashMapManager
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.impl.RealLoadingHandler
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.impl.RealPager
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.impl.RealPagingStateManager
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.impl.RealQueueManager
import org.mobilenativefoundation.storex.paging.runtime.internal.store.api.ItemStore
import org.mobilenativefoundation.storex.paging.runtime.internal.store.api.NormalizedStore
import org.mobilenativefoundation.storex.paging.runtime.internal.store.impl.ConcurrentItemStore
import org.mobilenativefoundation.storex.paging.runtime.internal.store.impl.ConcurrentPageStore
import org.mobilenativefoundation.storex.paging.runtime.internal.store.impl.ConcurrentStoreInvalidation
import org.mobilenativefoundation.storex.paging.runtime.internal.store.impl.RealNormalizedStore
import org.mobilenativefoundation.storex.paging.runtime.internal.store.impl.RealUpdatingItemPresenter
import org.mobilenativefoundation.storex.paging.runtime.internal.updatingItem.impl.RealUpdatingItemFactory
import org.mobilenativefoundation.storex.paging.runtime.internal.updatingItem.impl.RealUpdatingItemProvider

class PagingScopeBuilder<Id : Identifier<Id>, K : Comparable<K>, V : Identifiable<Id>>(
    private val pagingConfig: PagingConfig<Id, K>
) : PagingScope.Builder<Id, K, V> {

    private val logger = RealPagingLogger(pagingConfig.logging)
    private val actionsFlow = MutableSharedFlow<Action<K>>(replay = 20)

    private var initialState: PagingState<Id> = PagingState.initial()
    private var initialLoadParams = PagingSource.LoadParams(
        pagingConfig.initialKey,
        LoadStrategy.SkipCache,
        LoadDirection.Append
    )

    private lateinit var pagingSource: PagingSource<Id, K, V>
    private var coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO
    private val launchEffects = mutableListOf<LaunchEffect>()
    private val sideEffects = mutableListOf<SideEffect<Id, V>>()
    private val middleware = mutableListOf<Middleware<K>>()
    private var initialFetchingState = FetchingState<Id, K>()
    private var listSortAnalyzer: ListSortAnalyzer<Id> = DefaultListSortAnalyzer()
    private var fetchingStrategy: FetchingStrategy<Id, K> =
        DefaultFetchingStrategy(pagingConfig, logger, listSortAnalyzer)
    private var errorHandlingStrategy: ErrorHandlingStrategy = ErrorHandlingStrategy.RetryLast()

    private lateinit var itemMemoryCache: MutableMap<Id, V>
    private lateinit var pageMemoryCache: MutableMap<K, PagingSource.LoadResult.Data<Id, K, V>>
    private lateinit var itemPersistence: ItemPersistence<Id, K, V>
    private lateinit var pagePersistence: PagePersistence<Id, K, V>
    private lateinit var itemUpdater: Updater<Id, V, *>
    private lateinit var placeholderFactory: PlaceholderFactory<Id, K, V>

    override fun setInitialState(state: PagingState<Id>) = apply { initialState = state }
    override fun setInitialLoadParams(params: PagingSource.LoadParams<K>) =
        apply { initialLoadParams = params }

    override fun setPagingSource(source: PagingSource<Id, K, V>) = apply { pagingSource = source }
    override fun setCoroutineDispatcher(dispatcher: CoroutineDispatcher) =
        apply { coroutineDispatcher = dispatcher }

    override fun addLaunchEffect(effect: LaunchEffect) = apply { launchEffects.add(effect) }
    override fun addSideEffect(effect: SideEffect<Id, V>) = apply { sideEffects.add(effect) }
    override fun addMiddleware(mw: Middleware<K>) = apply { middleware.add(mw) }
    override fun setInitialFetchingState(state: FetchingState<Id, K>) =
        apply { initialFetchingState = state }

    override fun setFetchingStrategy(strategy: FetchingStrategy<Id, K>) =
        apply { fetchingStrategy = strategy }

    override fun setErrorHandlingStrategy(strategy: ErrorHandlingStrategy) =
        apply { errorHandlingStrategy = strategy }

    override fun setPlaceholderFactory(placeholderFactory: PlaceholderFactory<Id, K, V>): PagingScope.Builder<Id, K, V> =
        apply {
            this.placeholderFactory = placeholderFactory
        }

    override fun setItemMemoryCache(cache: MutableMap<Id, V>) = apply { itemMemoryCache = cache }
    override fun setPageMemoryCache(cache: MutableMap<K, PagingSource.LoadResult.Data<Id, K, V>>) =
        apply { pageMemoryCache = cache }

    override fun setItemPersistence(persistence: ItemPersistence<Id, K, V>) =
        apply { itemPersistence = persistence }

    override fun setPagePersistence(persistence: PagePersistence<Id, K, V>) =
        apply { pagePersistence = persistence }

    override fun setItemUpdater(updater: Updater<Id, V, *>) = apply { itemUpdater = updater }

    override fun build(): PagingScope<Id, K, V> {
        val operationManager = ConcurrentOperationManager<Id, K, V>()
        val fetchingStateHolder = ConcurrentFetchingStateHolder(initialFetchingState)

        val dataPersistence = RealDataPersistence(itemPersistence, pagePersistence)

        val linkedHashMapManager = RealLinkedHashMapManager(
            pageMemoryCache,
            itemMemoryCache,
            pagingConfig,
            dataPersistence,
            logger,
            placeholderFactory
        )
        val itemStore = ConcurrentItemStore(itemMemoryCache, itemPersistence, linkedHashMapManager)
        val store = buildStore(itemStore, fetchingStateHolder, linkedHashMapManager)
        val updatingItemPresenter =
            RealUpdatingItemPresenter(itemStore, itemUpdater, fetchingStateHolder)
        val updatingItemFactory = RealUpdatingItemFactory(updatingItemPresenter)
        val updatingItemProvider = RealUpdatingItemProvider(updatingItemFactory)
        val dispatcher = RealDispatcher(actionsFlow)
        val recompositionMode = RecompositionMode.ContextClock

        val pagingStateManager = RealPagingStateManager(initialState, logger)
        val queueManager = RealQueueManager<K>(logger)
        val operationApplier = ConcurrentOperationApplier(operationManager)
        val loadingHandler = createLoadingHandler(
            store = store,
            pagingStateManager = pagingStateManager,
            queueManager = queueManager,
            fetchingStateHolder = fetchingStateHolder,
            operationApplier = operationApplier
        )
        val coroutineScope = CoroutineScope(coroutineDispatcher)

        val pager = RealPager(
            recompositionMode = recompositionMode,
            fetchingStateHolder = fetchingStateHolder,
            launchEffects = launchEffects,
            fetchingStrategy = fetchingStrategy,
            initialLoadParams = initialLoadParams,
            store = store,
            actions = actionsFlow,
            logger = logger,
            pagingStateManager = pagingStateManager,
            queueManager = queueManager,
            loadingHandler = loadingHandler,
            coroutineScope = coroutineScope
        )

        return RealPagingScope(
            pager = pager,
            operationManager = operationManager,
            dispatcher = dispatcher,
            updatingItemProvider = updatingItemProvider
        )
    }

    private fun buildStore(
        itemStore: ItemStore<Id, K, V>,
        fetchingStateHolder: FetchingStateHolder<Id, K>,
        linkedHashMapManager: LinkedHashMapManager<Id, K, V>
    ): NormalizedStore<Id, K, V> {
        val pageStore = ConcurrentPageStore(
            pageMemoryCache,
            pagePersistence,
            fetchingStateHolder,
            pagingConfig,
            sideEffects,
            pagingSource,
            linkedHashMapManager,
            logger
        )
        val storeInvalidation = ConcurrentStoreInvalidation(itemStore, pageStore)
        return RealNormalizedStore(itemStore, pageStore, storeInvalidation)
    }

    private fun createLoadingHandler(
        store: NormalizedStore<Id, K, V>,
        pagingStateManager: PagingStateManager<Id>,
        queueManager: QueueManager<K>,
        fetchingStateHolder: FetchingStateHolder<Id, K>,
        operationApplier: OperationApplier<Id, K, V>
    ): LoadingHandler<Id, K, V> = RealLoadingHandler(
        store = store,
        pagingStateManager = pagingStateManager,
        queueManager = queueManager,
        fetchingStateHolder = fetchingStateHolder,
        errorHandlingStrategy = errorHandlingStrategy,
        middleware = middleware,
        operationApplier = operationApplier
    )
}