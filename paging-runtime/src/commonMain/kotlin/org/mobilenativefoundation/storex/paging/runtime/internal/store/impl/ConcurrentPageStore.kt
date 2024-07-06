package org.mobilenativefoundation.storex.paging.runtime.internal.store.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.mobilenativefoundation.storex.paging.persistence.PagePersistence
import org.mobilenativefoundation.storex.paging.persistence.PersistenceResult
import org.mobilenativefoundation.storex.paging.runtime.Identifiable
import org.mobilenativefoundation.storex.paging.runtime.Identifier
import org.mobilenativefoundation.storex.paging.runtime.ItemSnapshotList
import org.mobilenativefoundation.storex.paging.runtime.LoadDirection
import org.mobilenativefoundation.storex.paging.runtime.LoadStrategy
import org.mobilenativefoundation.storex.paging.runtime.PagingConfig
import org.mobilenativefoundation.storex.paging.runtime.PagingSource
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.api.FetchingStateHolder
import org.mobilenativefoundation.storex.paging.runtime.internal.store.api.PageLoadState
import org.mobilenativefoundation.storex.paging.runtime.internal.store.api.PageStore

/**
 * A thread-safe implementation of PageStore that manages pages of items.
 *
 * This class uses a combination of in-memory cache and a persistent storage layer to provide
 * efficient and reliable page management. It ensures thread-safety through the use
 * of a Mutex for all operations that modify shared state.
 *
 * @param Id The type of the item identifier.
 * @param K The type of the paging key.
 * @param V The type of the item value.
 * @property pageMemoryCache In-memory cache for quick access to pages.
 * @property pagePersistence Persistent storage layer for pages.
 * @property fetchingStateHolder Holder for the current fetching state.
 * @property pagingConfig Configuration for paging behavior.
 */
internal class ConcurrentPageStore<Id : Identifier<Id>, K : Comparable<K>, V : Identifiable<Id>>(
    private val pageMemoryCache: MutableMap<K, PagingSource.LoadResult.Data<Id, K, V>>,
    private val pagePersistence: PagePersistence<Id, K, V>,
    private val fetchingStateHolder: FetchingStateHolder<Id, K>,
    private val pagingConfig: PagingConfig<Id, K>
) : PageStore<Id, K, V> {

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
    override suspend fun loadPage(params: PagingSource.LoadParams<K>): Flow<PageLoadState<Id, K, V>> =
        flow {
            mutex.withLock {
                // Emit initial processing state
                emit(PageLoadState.Processing())

                // Check if placeholders should be added
                if (shouldAddPlaceholders(params)) {
                    addPlaceholders(params)
                }

                // Attempt to load from memory cache
                val cachedPage = pageMemoryCache[params.key]
                if (cachedPage != null) {
                    emit(PageLoadState.Loading.memoryCache())
                    emit(createSuccessState(cachedPage, PageLoadState.Success.Source.MemoryCache))
                    return@flow
                }

                // Attempt to load from persistent storage
                when (val persistenceResult = pagePersistence.getPage(params)) {
                    is PersistenceResult.Success -> {
                        val persistedPage = persistenceResult.data
                        if (persistedPage != null) {
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
                    }

                    is PersistenceResult.Error -> {
                        // Log the error, but continue to try remote fetching
                        println("Error loading page from persistence: ${persistenceResult.message}")
                    }
                }

                // If we reach here, we need to fetch from remote
                emit(PageLoadState.Loading.remote())
                when (val fetchResult = fetchPageFromRemote(params)) {
                    is PersistenceResult.Success -> {
                        val fetchedPage = fetchResult.data
                        pageMemoryCache[params.key] = fetchedPage
                        pagePersistence.savePage(params, fetchedPage)
                        emit(createSuccessState(fetchedPage, PageLoadState.Success.Source.Network))
                    }

                    is PersistenceResult.Error -> {
                        emit(PageLoadState.Error.Exception(Exception(fetchResult.message), true))
                    }
                }
            }
        }

    /**
     * Clears the page associated with the given key.
     *
     * @param key The key of the page to clear.
     */
    override suspend fun clearPage(key: K) {
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
    suspend fun clearAllPages(): PersistenceResult<Unit> = mutex.withLock {
        pageMemoryCache.clear()
        pagePersistence.clearAllPages()
    }

    /**
     * Determines if placeholders should be added for the given load parameters.
     *
     * @param params The load parameters to check.
     * @return True if placeholders should be added, false otherwise.
     */
    private fun shouldAddPlaceholders(params: PagingSource.LoadParams<K>): Boolean {
        return pagingConfig.placeholderId != null && params.strategy != LoadStrategy.LocalOnly
    }

    /**
     * Adds placeholders for the given load parameters.
     *
     * @param params The load parameters for which to add placeholders.
     */
    private fun addPlaceholders(params: PagingSource.LoadParams<K>) {
        // Implementation of placeholder addition is omitted for brevity
        // This would typically involve creating a page of placeholder items
        // and adding it to the pageMemoryCache
    }

    /**
     * Creates a success state from a loaded page.
     *
     * @param page The loaded page.
     * @param source The source from which the page was loaded.
     * @return A PageLoadState.Success instance.
     */
    private fun createSuccessState(
        page: PagingSource.LoadResult.Data<Id, K, V>,
        source: PageLoadState.Success.Source
    ): PageLoadState.Success<Id, K, V> {
        return PageLoadState.Success(
            snapshot = ItemSnapshotList(page.items),
            isTerminal = page.nextKey == null && page.prevKey == null,
            source = source,
            nextKey = page.nextKey,
            prevKey = page.prevKey
        )
    }

    /**
     * Fetches a page from a remote source.
     *
     * This method is a placeholder and should be replaced with actual remote fetching logic.
     *
     * @param params The load parameters for the page to fetch.
     * @return A PersistenceResult containing the fetched page or an error.
     */
    private suspend fun fetchPageFromRemote(params: PagingSource.LoadParams<K>): PersistenceResult<PagingSource.LoadResult.Data<Id, K, V>> {
        // This is a placeholder. In a real implementation, this would involve
        // calling an API or other remote data source.
        TODO("Implement remote fetching logic")
    }
}