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
import org.mobilenativefoundation.storex.paging.runtime.Comparator
import org.mobilenativefoundation.storex.paging.runtime.ErrorHandlingStrategy
import org.mobilenativefoundation.storex.paging.runtime.FetchingState
import org.mobilenativefoundation.storex.paging.runtime.IdExtractor
import org.mobilenativefoundation.storex.paging.runtime.LoadDirection
import org.mobilenativefoundation.storex.paging.runtime.LoadStrategy
import org.mobilenativefoundation.storex.paging.runtime.Operation
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
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.impl.DefaultFetchingStrategy
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.impl.DefaultListSortAnalyzer
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.impl.RealLinkedHashMapManager
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.impl.RealLoadingHandler
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.impl.RealMutableOperationPipeline
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

class PagingScopeBuilder<ItemId : Any, PageRequestKey : Any, ItemValue : Any>(
    private val pagingConfig: PagingConfig<ItemId, PageRequestKey>,
) : PagingScope.Builder<ItemId, PageRequestKey, ItemValue> {

    private val logger = RealPagingLogger(pagingConfig.logging)
    private val actionsFlow = MutableSharedFlow<Action<ItemId, PageRequestKey, ItemValue>>(replay = 20)

    private var initialState: PagingState<ItemId, PageRequestKey, ItemValue> = PagingState.initial()
    private var initialLoadParams = PagingSource.LoadParams(
        pagingConfig.initialKey,
        LoadStrategy.SkipCache,
        LoadDirection.Append
    )

    private lateinit var pagingSource: PagingSource<ItemId, PageRequestKey, ItemValue>
    private lateinit var itemIdComparator: Comparator<ItemId>
    private lateinit var pageRequestKeyComparator: Comparator<PageRequestKey>
    private lateinit var idExtractor: IdExtractor<ItemId, ItemValue>
    private var coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO
    private val launchEffects = mutableListOf<LaunchEffect>()
    private val sideEffects = mutableListOf<SideEffect<ItemId, ItemValue>>()
    private val middleware = mutableListOf<Middleware<PageRequestKey>>()
    private var initialFetchingState = FetchingState<ItemId, PageRequestKey>()
    private var listSortAnalyzer: ListSortAnalyzer<ItemId> = DefaultListSortAnalyzer(itemIdComparator)
    private var fetchingStrategy: FetchingStrategy<ItemId, PageRequestKey, ItemValue> =
        DefaultFetchingStrategy(pagingConfig, logger, listSortAnalyzer, itemIdComparator)
    private var errorHandlingStrategy: ErrorHandlingStrategy = ErrorHandlingStrategy.RetryLast()
    private var initialOperations: MutableList<Operation<ItemId, PageRequestKey, ItemValue>> = mutableListOf()

    private lateinit var itemMemoryCache: MutableMap<ItemId, ItemValue>
    private lateinit var pageMemoryCache: MutableMap<PageRequestKey, PagingSource.LoadResult.Data<ItemId, PageRequestKey, ItemValue>>
    private lateinit var itemPersistence: ItemPersistence<ItemId, PageRequestKey, ItemValue>
    private lateinit var pagePersistence: PagePersistence<ItemId, PageRequestKey, ItemValue>
    private lateinit var itemUpdater: Updater<ItemId, ItemValue, *>
    private lateinit var placeholderFactory: PlaceholderFactory<ItemId, PageRequestKey, ItemValue>

    override fun setInitialState(state: PagingState<ItemId, PageRequestKey, ItemValue>) = apply { initialState = state }
    override fun setInitialLoadParams(params: PagingSource.LoadParams<PageRequestKey>) =
        apply { initialLoadParams = params }

    override fun setPagingSource(source: PagingSource<ItemId, PageRequestKey, ItemValue>) = apply { pagingSource = source }
    override fun setCoroutineDispatcher(dispatcher: CoroutineDispatcher) =
        apply { coroutineDispatcher = dispatcher }

    override fun addLaunchEffect(effect: LaunchEffect) = apply { launchEffects.add(effect) }
    override fun addSideEffect(effect: SideEffect<ItemId, ItemValue>) = apply { sideEffects.add(effect) }
    override fun addMiddleware(mw: Middleware<PageRequestKey>) = apply { middleware.add(mw) }
    override fun setInitialFetchingState(state: FetchingState<ItemId, PageRequestKey>) =
        apply { initialFetchingState = state }

    override fun setFetchingStrategy(strategy: FetchingStrategy<ItemId, PageRequestKey, ItemValue>) =
        apply { fetchingStrategy = strategy }

    override fun setErrorHandlingStrategy(strategy: ErrorHandlingStrategy) =
        apply { errorHandlingStrategy = strategy }

    override fun setPlaceholderFactory(placeholderFactory: PlaceholderFactory<ItemId, PageRequestKey, ItemValue>): PagingScope.Builder<ItemId, PageRequestKey, ItemValue> =
        apply {
            this.placeholderFactory = placeholderFactory
        }

    override fun setItemMemoryCache(cache: MutableMap<ItemId, ItemValue>) = apply { itemMemoryCache = cache }
    override fun setPageMemoryCache(cache: MutableMap<PageRequestKey, PagingSource.LoadResult.Data<ItemId, PageRequestKey, ItemValue>>) =
        apply { pageMemoryCache = cache }

    override fun setItemPersistence(persistence: ItemPersistence<ItemId, PageRequestKey, ItemValue>) =
        apply { itemPersistence = persistence }

    override fun setPagePersistence(persistence: PagePersistence<ItemId, PageRequestKey, ItemValue>) =
        apply { pagePersistence = persistence }

    override fun setItemUpdater(updater: Updater<ItemId, ItemValue, *>) = apply { itemUpdater = updater }

    override fun setInitialOperations(operations: List<Operation<ItemId, PageRequestKey, ItemValue>>): PagingScope.Builder<ItemId, PageRequestKey, ItemValue> =
        apply {
            this.initialOperations = operations.toMutableList()
        }

    override fun build(): PagingScope<ItemId, PageRequestKey, ItemValue> {
        val fetchingStateHolder = ConcurrentFetchingStateHolder(initialFetchingState, itemIdComparator, pageRequestKeyComparator)

        val dataPersistence = RealDataPersistence(itemPersistence, pagePersistence)

        val linkedHashMapManager = RealLinkedHashMapManager(
            pageMemoryCache,
            itemMemoryCache,
            pagingConfig,
            dataPersistence,
            logger,
            placeholderFactory,
            idExtractor
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
        val queueManager = RealQueueManager(logger, pageRequestKeyComparator)
        val mutableOperationPipeline = RealMutableOperationPipeline(initialOperations)
        val operationApplier = ConcurrentOperationApplier(mutableOperationPipeline)
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
            coroutineScope = coroutineScope,
            mutableOperationPipeline = mutableOperationPipeline
        )

        return RealPagingScope(
            pager = pager,
            dispatcher = dispatcher,
            updatingItemProvider = updatingItemProvider
        )
    }

    private fun buildStore(
        itemStore: ItemStore<ItemId, PageRequestKey, ItemValue>,
        fetchingStateHolder: FetchingStateHolder<ItemId, PageRequestKey>,
        linkedHashMapManager: LinkedHashMapManager<ItemId, PageRequestKey, ItemValue>
    ): NormalizedStore<ItemId, PageRequestKey, ItemValue> {
        val pageStore = ConcurrentPageStore(
            pageMemoryCache,
            pagePersistence,
            fetchingStateHolder,
            pagingConfig,
            sideEffects,
            pagingSource,
            linkedHashMapManager,
            logger,
            idExtractor
        )
        val storeInvalidation = ConcurrentStoreInvalidation(itemStore, pageStore)
        return RealNormalizedStore(itemStore, pageStore, storeInvalidation)
    }

    private fun createLoadingHandler(
        store: NormalizedStore<ItemId, PageRequestKey, ItemValue>,
        pagingStateManager: PagingStateManager<ItemId, PageRequestKey, ItemValue>,
        queueManager: QueueManager<PageRequestKey>,
        fetchingStateHolder: FetchingStateHolder<ItemId, PageRequestKey>,
        operationApplier: OperationApplier<ItemId, PageRequestKey, ItemValue>
    ): LoadingHandler<ItemId, PageRequestKey, ItemValue> = RealLoadingHandler(
        store = store,
        pagingStateManager = pagingStateManager,
        queueManager = queueManager,
        fetchingStateHolder = fetchingStateHolder,
        errorHandlingStrategy = errorHandlingStrategy,
        middleware = middleware,
        operationApplier = operationApplier
    )
}