package org.mobilenativefoundation.storex.paging.runtime.internal.pager.api

import kotlinx.coroutines.flow.Flow
import org.mobilenativefoundation.storex.paging.persistence.api.PersistenceResult
import org.mobilenativefoundation.storex.paging.runtime.PagingSource

internal interface LinkedHashMapManager<ItemId: Any, PageRequestKey: Any, ItemValue: Any> {
    suspend fun isInFlight(key: PageRequestKey): Boolean

    suspend fun isCached(key: PageRequestKey): Boolean

    suspend fun isInDatabase(params: PagingSource.LoadParams<PageRequestKey>): Boolean


    suspend fun appendPlaceholders(params: PagingSource.LoadParams<PageRequestKey>)
    suspend fun prependPlaceholders(params: PagingSource.LoadParams<PageRequestKey>)

    suspend fun getItem(id: ItemId): ItemValue?

    suspend fun getCachedItem(id: ItemId): ItemValue?
    suspend fun getPersistedItem(id: ItemId): ItemValue?

    suspend fun getCachedPage(key: PageRequestKey):  PagingSource. LoadResult. Data<ItemId, PageRequestKey, ItemValue>?
    suspend fun getPersistedPage(params: PagingSource.LoadParams<PageRequestKey>):  PagingSource. LoadResult. Data<ItemId, PageRequestKey, ItemValue>?

    suspend fun saveItem(item: ItemValue) : PersistenceResult<Unit>
    suspend fun getItemsInOrder(): List<ItemValue?>

    suspend fun putPageNode(key: PageRequestKey, pageNode: PageNode<PageRequestKey>)

    suspend fun appendPage(
        params: PagingSource.LoadParams<PageRequestKey>,
        loadResult: PagingSource.LoadResult.Data<ItemId, PageRequestKey, ItemValue>
    )

    suspend fun prependPage(
        params: PagingSource.LoadParams<PageRequestKey>,
        loadResult: PagingSource.LoadResult.Data<ItemId, PageRequestKey, ItemValue>
    )

    suspend fun removeItem(id: ItemId): PersistenceResult<Unit>
    suspend fun removeAllItems(): PersistenceResult<Unit>

    suspend fun removePage(key: PageRequestKey)
    suspend fun removeAllPages(): PersistenceResult<Unit>

    suspend fun invalidate()

    suspend fun queryItems(predicate: (ItemValue) -> Boolean): PersistenceResult<List<ItemValue>>

    fun observeItem(id: ItemId): Flow<ItemValue?>

    data class PageNode<K : Any>(
        val key: K,
        var isPlaceholder: Boolean = false,
        var isInFlight: Boolean = false,
        var prev: PageNode<K>? = null,
        var next: PageNode<K>? = null
    )
}