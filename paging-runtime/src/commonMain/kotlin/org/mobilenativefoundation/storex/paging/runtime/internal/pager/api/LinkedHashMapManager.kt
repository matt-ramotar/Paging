package org.mobilenativefoundation.storex.paging.runtime.internal.pager.api

import kotlinx.coroutines.flow.Flow
import org.mobilenativefoundation.storex.paging.persistence.api.PersistenceResult
import org.mobilenativefoundation.storex.paging.runtime.Identifiable
import org.mobilenativefoundation.storex.paging.runtime.Identifier
import org.mobilenativefoundation.storex.paging.runtime.PagingSource

internal interface LinkedHashMapManager<Id : Identifier<Id>, K : Comparable<K>, V : Identifiable<Id>> {
    suspend fun isInFlight(key: K): Boolean

    suspend fun isCached(key: K): Boolean

    suspend fun isInDatabase(params: PagingSource.LoadParams<K>): Boolean


    suspend fun appendPlaceholders(params: PagingSource.LoadParams<K>)
    suspend fun prependPlaceholders(params: PagingSource.LoadParams<K>)

    suspend fun getItem(id: Id): V?

    suspend fun getCachedItem(id: Id): V?
    suspend fun getPersistedItem(id: Id): V?

    suspend fun getCachedPage(key: K):  PagingSource. LoadResult. Data<Id, K, V>?
    suspend fun getPersistedPage(key: K):  PagingSource. LoadResult. Data<Id, K, V>?

    suspend fun saveItem(item: V) : PersistenceResult<Unit>
    suspend fun getItemsInOrder(): List<V?>

    suspend fun putPageNode(key: K, pageNode: PageNode<K>)

    suspend fun appendPage(
        params: PagingSource.LoadParams<K>,
        loadResult: PagingSource.LoadResult.Data<Id, K, V>
    )

    suspend fun prependPage(
        params: PagingSource.LoadParams<K>,
        loadResult: PagingSource.LoadResult.Data<Id, K, V>
    )

    suspend fun removeItem(id: Id): PersistenceResult<Unit>
    suspend fun removeAllItems(): PersistenceResult<Unit>

    suspend fun removePage(key: K)
    suspend fun removeAllPages(): PersistenceResult<Unit>

    suspend fun invalidate()

    suspend fun queryItems(predicate: (V) -> Boolean): PersistenceResult<List<V>>

    fun observeItem(id: Id): Flow<V?>

    data class PageNode<K : Any>(
        val key: K,
        var isPlaceholder: Boolean = false,
        var isInFlight: Boolean = false,
        var prev: PageNode<K>? = null,
        var next: PageNode<K>? = null
    )
}