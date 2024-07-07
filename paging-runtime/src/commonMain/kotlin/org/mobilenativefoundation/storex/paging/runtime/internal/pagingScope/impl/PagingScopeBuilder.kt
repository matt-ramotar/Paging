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
import org.mobilenativefoundation.storex.paging.persistence.ItemPersistence
import org.mobilenativefoundation.storex.paging.persistence.PagePersistence
import org.mobilenativefoundation.storex.paging.runtime.Action
import org.mobilenativefoundation.storex.paging.runtime.Dispatcher
import org.mobilenativefoundation.storex.paging.runtime.ErrorHandlingStrategy
import org.mobilenativefoundation.storex.paging.runtime.FetchingState
import org.mobilenativefoundation.storex.paging.runtime.Identifiable
import org.mobilenativefoundation.storex.paging.runtime.Identifier
import org.mobilenativefoundation.storex.paging.runtime.LoadDirection
import org.mobilenativefoundation.storex.paging.runtime.LoadStrategy
import org.mobilenativefoundation.storex.paging.runtime.OperationManager
import org.mobilenativefoundation.storex.paging.runtime.PagingConfig
import org.mobilenativefoundation.storex.paging.runtime.PagingScope
import org.mobilenativefoundation.storex.paging.runtime.PagingSource
import org.mobilenativefoundation.storex.paging.runtime.PagingState
import org.mobilenativefoundation.storex.paging.runtime.RecompositionMode
import org.mobilenativefoundation.storex.paging.runtime.UpdatingItemProvider
import org.mobilenativefoundation.storex.paging.runtime.internal.logger.api.PagingLogger
import org.mobilenativefoundation.storex.paging.runtime.internal.logger.impl.RealPagingLogger
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.api.FetchingStateHolder
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.api.LoadingHandler
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.api.OperationApplier
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.api.PagingStateManager
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.api.QueueManager
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.impl.ConcurrentFetchingStateHolder
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.impl.ConcurrentOperationApplier
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.impl.ConcurrentOperationManager
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.impl.DefaultFetchingStrategy
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.impl.RealLoadingHandler
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.impl.RealPager
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.impl.RealPagingStateManager
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.impl.RealQueueManager
import org.mobilenativefoundation.storex.paging.runtime.internal.store.api.ItemStore
import org.mobilenativefoundation.storex.paging.runtime.internal.store.api.NormalizedStore
import org.mobilenativefoundation.storex.paging.runtime.internal.store.api.UpdatingItemPresenter
import org.mobilenativefoundation.storex.paging.runtime.internal.store.impl.ConcurrentItemStore
import org.mobilenativefoundation.storex.paging.runtime.internal.store.impl.ConcurrentPageStore
import org.mobilenativefoundation.storex.paging.runtime.internal.store.impl.ConcurrentStoreInvalidation
import org.mobilenativefoundation.storex.paging.runtime.internal.store.impl.RealNormalizedStore
import org.mobilenativefoundation.storex.paging.runtime.internal.store.impl.RealUpdatingItemPresenter
import org.mobilenativefoundation.storex.paging.runtime.internal.updatingItem.api.UpdatingItemFactory
import org.mobilenativefoundation.storex.paging.runtime.internal.updatingItem.impl.RealUpdatingItemFactory
import org.mobilenativefoundation.storex.paging.runtime.internal.updatingItem.impl.RealUpdatingItemProvider

class PagingScopeBuilder<Id : Identifier<Id>, K : Comparable<K>, V : Identifiable<Id>>(
    private val pagingConfig: PagingConfig<Id, K>
) : PagingScope.Builder<Id, K, V> {

    // Non configurable
    private val logger: PagingLogger = RealPagingLogger(pagingConfig)
    private val actionsFlow: MutableSharedFlow<Action<K>> = MutableSharedFlow(replay = 20)


    // Configurable
    private var initialState: PagingState<Id> = PagingState.initial()
    private var initialLoadParams = PagingSource.LoadParams(
        pagingConfig.initialKey,
        strategy = LoadStrategy.SkipCache,
        direction = LoadDirection.Append
    )

    private var storexPagingSource: PagingSource<Id, K, V>? = null
    private var androidxPagingSource: PagingSource<Id, K, V>? = null
    private var coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO
    private val launchEffects: MutableList<LaunchEffect> = mutableListOf()
    private val sideEffects: MutableList<SideEffect<Id, V>> = mutableListOf()
    private val middleware: MutableList<Middleware<K>> = mutableListOf()
    private var initialFetchingState: FetchingState<Id, K> = FetchingState()
    private var fetchingStrategy: FetchingStrategy<Id, K> =
        DefaultFetchingStrategy(pagingConfig, logger)
    private var errorHandlingStrategy: ErrorHandlingStrategy = ErrorHandlingStrategy.RetryLast()


    // TODO(): Use Store cache
    private lateinit var itemMemoryCache: MutableMap<Id, V>
    private lateinit var pageMemoryCache: MutableMap<K, PagingSource.LoadResult.Data<Id, K, V>>

    // TODO(): Should this be SOT?
    private lateinit var itemPersistence: ItemPersistence<Id, K, V>
    private lateinit var pagePersistence: PagePersistence<Id, K, V>

    private lateinit var itemUpdater: Updater<Id, V, *>

    // Public builders


    // Private builders

    private fun createPagingStateManager(): PagingStateManager<Id> {
        return RealPagingStateManager(initialState)
    }

    private fun createQueueManager(): QueueManager<K> {
        return RealQueueManager()
    }

    private fun createOperationApplier(
        operationManager: OperationManager<Id, K, V>
    ): OperationApplier<Id, K, V> {
        return ConcurrentOperationApplier(operationManager)
    }

    private fun createLoadingHandler(
        store: NormalizedStore<Id, K, V>,
        pagingStateManager: PagingStateManager<Id>,
        queueManager: QueueManager<K>,
        fetchingStateHolder: FetchingStateHolder<Id, K>,
        operationApplier: OperationApplier<Id, K, V>
    ): LoadingHandler<Id, K, V> {
        return RealLoadingHandler(
            store = store,
            pagingStateManager = pagingStateManager,
            queueManager = queueManager,
            fetchingStateHolder = fetchingStateHolder,
            errorHandlingStrategy = errorHandlingStrategy,
            middleware = middleware,
            operationApplier = operationApplier
        )
    }

    private fun createCoroutineScope(): CoroutineScope {
        return CoroutineScope(coroutineDispatcher)
    }


    private fun buildStore(
        itemStore: ItemStore<Id, V>,
        fetchingStateHolder: FetchingStateHolder<Id, K>,
    ): NormalizedStore<Id, K, V> {

        val pageStore = ConcurrentPageStore(
            pageMemoryCache = pageMemoryCache,
            pagePersistence = pagePersistence,
            fetchingStateHolder = fetchingStateHolder,
            pagingConfig = pagingConfig,
            sideEffects = sideEffects
        )

        val storeInvalidation = ConcurrentStoreInvalidation(
            itemStore = itemStore,
            pageStore = pageStore
        )

        return RealNormalizedStore(
            itemStore = itemStore,
            pageStore = pageStore,
            storeInvalidation = storeInvalidation
        )
    }

    override fun build(): PagingScope<Id, K, V> {

        val operationManager: OperationManager<Id, K, V> = ConcurrentOperationManager()

        val fetchingStateHolder: FetchingStateHolder<Id, K> =
            ConcurrentFetchingStateHolder(initialFetchingState)

        val itemStore = ConcurrentItemStore(
            itemMemoryCache = itemMemoryCache,
            itemPersistence = itemPersistence,
        )

        val store = buildStore(itemStore, fetchingStateHolder)

        val updatingItemPresenter: UpdatingItemPresenter<Id, V> =
            RealUpdatingItemPresenter(
                itemStore,
                itemUpdater,
                fetchingStateHolder
            )

        val updatingItemFactory: UpdatingItemFactory<Id, V> =
            RealUpdatingItemFactory(updatingItemPresenter)

        val updatingItemProvider: UpdatingItemProvider<Id, V> =
            RealUpdatingItemProvider(updatingItemFactory)

        val dispatcher: Dispatcher<K> = RealDispatcher(actionsFlow)

        val recompositionMode: RecompositionMode = RecompositionMode.ContextClock


        val pagingStateManager = createPagingStateManager()
        val queueManager = createQueueManager()
        val operationApplier = createOperationApplier(operationManager)
        val loadingHandler = createLoadingHandler(
            store = store,
            pagingStateManager = pagingStateManager,
            queueManager = queueManager,
            fetchingStateHolder = fetchingStateHolder,
            operationApplier = operationApplier
        )
        val coroutineScope = createCoroutineScope()

        val pager = RealPager(
            recompositionMode = recompositionMode,
            fetchingStateHolder = fetchingStateHolder,
            launchEffects = launchEffects,
            fetchingStrategy = fetchingStrategy,
            initialLoadParams = initialLoadParams,
            store = store,
            actions = actionsFlow,
            config = pagingConfig,
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
}