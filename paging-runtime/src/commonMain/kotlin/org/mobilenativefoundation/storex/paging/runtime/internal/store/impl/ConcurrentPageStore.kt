package org.mobilenativefoundation.storex.paging.runtime.internal.store.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.mobilenativefoundation.storex.paging.custom.SideEffect
import org.mobilenativefoundation.storex.paging.persistence.api.PagePersistence
import org.mobilenativefoundation.storex.paging.persistence.api.PersistenceResult
import org.mobilenativefoundation.storex.paging.runtime.IdExtractor
import org.mobilenativefoundation.storex.paging.runtime.ItemSnapshotList
import org.mobilenativefoundation.storex.paging.runtime.LoadDirection
import org.mobilenativefoundation.storex.paging.runtime.LoadStrategy
import org.mobilenativefoundation.storex.paging.runtime.PagingConfig
import org.mobilenativefoundation.storex.paging.runtime.PagingSource
import org.mobilenativefoundation.storex.paging.runtime.internal.logger.api.PagingLogger
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.api.FetchingStateHolder
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.api.LinkedHashMapManager
import org.mobilenativefoundation.storex.paging.runtime.internal.pagingScope.api.PageMemoryCache
import org.mobilenativefoundation.storex.paging.runtime.internal.store.api.PageLoadState
import org.mobilenativefoundation.storex.paging.runtime.internal.store.api.PageStore

/**
 * A thread-safe implementation of PageStore that manages pages of items.
 *
 * This class uses a combination of in-memory cache and a persistent storage layer to provide
 * efficient and reliable page management. It ensures thread-safety through the use
 * of a Mutex for all operations that modify shared state.
 *
 * @param ItemId The type of the item identifier.
 * @param PageRequestKey The type of the paging key.
 * @param ItemValue The type of the item value.
 * @property pageMemoryCache In-memory cache for quick access to pages.
 * @property pagePersistence Persistent storage layer for pages.
 * @property fetchingStateHolder Holder for the current fetching state.
 * @property pagingConfig Configuration for paging behavior.
 */
internal class ConcurrentPageStore<ItemId : Any, PageRequestKey : Any, ItemValue : Any>(
    private val pageMemoryCache: PageMemoryCache<ItemId, PageRequestKey, ItemValue>,
    private val pagePersistence: PagePersistence<ItemId, PageRequestKey, ItemValue>,
    private val fetchingStateHolder: FetchingStateHolder<ItemId, PageRequestKey>,
    private val pagingConfig: PagingConfig<ItemId, PageRequestKey>,
    private val sideEffects: List<SideEffect<ItemId, ItemValue>>,
    private val pagingSource: PagingSource<ItemId, PageRequestKey, ItemValue>,
    private val linkedHashMapManager: LinkedHashMapManager<ItemId, PageRequestKey, ItemValue>,
    private val logger: PagingLogger,
    private val idExtractor: IdExtractor<ItemId, ItemValue>
) : PageStore<ItemId, PageRequestKey, ItemValue> {

    // Mutex for ensuring thread-safe access to shared resources
    private val mutex = Mutex()

    /**
     * Loads a page of data based on the given load parameters.
     *
     * This method implements a cache-first strategy, falling back to persistent storage
     * and then to remote fetching if necessary.
     *
     * @param params The load parameters for the page.
     * @return A Flow emitting the PageLoadState for the requested page.
     */
    override suspend fun loadPage(params: PagingSource.LoadParams<PageRequestKey>): Flow<PageLoadState<ItemId, PageRequestKey, ItemValue>> =
        flow {
            mutex.withLock {
                // Emit initial processing state
                emit(PageLoadState.Processing())

                // Check if placeholders should be added
                if (shouldAddPlaceholders(params)) {
                    addPlaceholders(params)
                }

                // Attempt to load from memory cache
                logger.debug("Attempting to load page from memory cache")
                val cachedPage = linkedHashMapManager.getCachedPage(params.key)
                if (cachedPage != null) {
                    logger.debug("Found page in memory cache: $cachedPage")
                    emit(PageLoadState.Loading.memoryCache())
                    emit(createSuccessState(cachedPage, PageLoadState.Success.Source.MemoryCache))
                    return@flow
                }

                // Attempt to load from persistent storage
                logger.debug("Attempting to load page from persistent storage")
                val persistedPage = linkedHashMapManager.getPersistedPage(params)

                if (persistedPage != null) {
                    logger.debug("Found page in persistent storage: $persistedPage")
                    emit(PageLoadState.Loading.database())
                    pageMemoryCache[params.key] = persistedPage
                    emit(
                        createSuccessState(
                            persistedPage,
                            PageLoadState.Success.Source.Database
                        )
                    )
                    return@flow
                }

                // If we reach here, we need to fetch from remote
                logger.debug("Attempting to load page from remote")
                emit(PageLoadState.Loading.remote())

                when (val loadResult = loadPageFromRemote(params)) {

                    is PagingSource.LoadResult.Data -> {

                        logger.debug("Success loading page from remote: $loadResult")

                        when (params.direction) {
                            LoadDirection.Prepend -> linkedHashMapManager.prependPage(
                                params,
                                loadResult
                            )

                            LoadDirection.Append -> linkedHashMapManager.appendPage(
                                params,
                                loadResult
                            )
                        }

                        emit(createSuccessState(loadResult, PageLoadState.Success.Source.Network))
                    }

                    is PagingSource.LoadResult.Error.Exception -> {
                        logger.debug("Error loading page from remote: ${loadResult.error}")
                        emit(PageLoadState.Error.Exception(loadResult.error, true))
                    }

                    is PagingSource.LoadResult.Error.Message -> {
                        logger.debug("Error loading page from remote: ${loadResult.error}")
                        emit(PageLoadState.Error.Exception(Exception(loadResult.error), true))
                    }
                }
            }
        }

    /**
     * Clears the page associated with the given key.
     *
     * @param key The key of the page to clear.
     */
    override suspend fun clearPage(key: PageRequestKey) {
        logger.debug("Clearing page for key: $key")
        mutex.withLock {
            pageMemoryCache.remove(key)
            pagePersistence.removePage(
                PagingSource.LoadParams(
                    key,
                    LoadStrategy.SkipCache,
                    LoadDirection.Append
                )
            )
        }
    }

    /**
     * Clears all pages from the memory cache and the persistent storage.
     *
     * @return A PersistenceResult indicating success or failure of the operation.
     */
    override suspend fun clearAllPages(): PersistenceResult<Unit> = mutex.withLock {
        logger.debug("Clearing all pages")
        pageMemoryCache.clear()
        pagePersistence.clearAllPages()
    }

    /**
     * Determines if placeholders should be added for the given load parameters.
     *
     * @param params The load parameters to check.
     * @return True if placeholders should be added, false otherwise.
     */
    private fun shouldAddPlaceholders(params: PagingSource.LoadParams<PageRequestKey>): Boolean {
        val placeholdersAreEnabled = pagingConfig.placeholderId != null
        val mightFetchFromRemote = params.strategy != LoadStrategy.LocalOnly
        val should = placeholdersAreEnabled && mightFetchFromRemote
        logger.debug("Should add placeholders: $should")
        return should
    }

    /**
     * Adds placeholders for the given load parameters.
     *
     * @param params The load parameters for which to add placeholders.
     */
    private suspend fun addPlaceholders(params: PagingSource.LoadParams<PageRequestKey>) {
        logger.debug("Adding placeholders for params: $params")
        when (params.direction) {
            LoadDirection.Prepend -> linkedHashMapManager.prependPlaceholders(params)
            LoadDirection.Append -> linkedHashMapManager.appendPlaceholders(params)
        }
    }

    /**
     * Creates a success state from a loaded page.
     *
     * @param page The loaded page.
     * @param source The source from which the page was loaded.
     * @return A PageLoadState.Success instance.
     */
    private fun createSuccessState(
        page: PagingSource.LoadResult.Data<ItemId, PageRequestKey, ItemValue>,
        source: PageLoadState.Success.Source
    ): PageLoadState.Success<ItemId, PageRequestKey, ItemValue> {

        val snapshot = takeSnapshot(page)

        return PageLoadState.Success(
            snapshot = snapshot,
            isTerminal = page.nextKey == null && page.prevKey == null,
            source = source,
            nextKey = page.nextKey,
            prevKey = page.prevKey
        ).also {
            launchSideEffects(snapshot)
        }
    }

    private fun takeSnapshot(page: PagingSource.LoadResult.Data<ItemId, PageRequestKey, ItemValue>): ItemSnapshotList<ItemId, ItemValue> {
        return ItemSnapshotList(page.items, idExtractor)
    }

    private fun launchSideEffects(snapshot: ItemSnapshotList<ItemId, ItemValue>) {
        sideEffects.forEachIndexed { index, sideEffect ->
            logger.debug("Launching side effect #$index")
            sideEffect.invoke(snapshot)
        }
    }

    /**
     * Fetches a page from a remote source.
     * @param params The load parameters for the page to fetch.
     * @return A [PagingSource.LoadResult] instance.
     */
    private suspend fun loadPageFromRemote(params: PagingSource.LoadParams<PageRequestKey>): PagingSource.LoadResult<ItemId, PageRequestKey, ItemValue> {
        return pagingSource.load(params)
    }
}