package org.mobilenativefoundation.store5.cache

import org.mobilenativefoundation.store.cache5.Cache
import org.mobilenativefoundation.store5.core.Identifiable
import org.mobilenativefoundation.storex.paging.PagingSource

class NormalizingCache<Id : Comparable<Id>, K : Any, V : Identifiable<Id>, E : Any>(
    private val normalizedCache: NormalizedCache<Id, K, V>,
    private val delegate: Cache<PagingSource.LoadParams<K>, PagingSource.LoadResult.Data<Id, K, V, E>>
) : Cache<PagingSource.LoadParams<K>, PagingSource.LoadResult.Data<Id, K, V, E>> by delegate {
    override fun getOrPut(
        key: PagingSource.LoadParams<K>,
        valueProducer: () -> PagingSource.LoadResult.Data<Id, K, V, E>
    ): PagingSource.LoadResult.Data<Id, K, V, E> {
        val data = delegate.getIfPresent(key)

        if (data == null) {
            val newData = valueProducer()
            put(key, newData)
        }

        return delegate.getIfPresent(key)!!
    }

    override fun put(key: PagingSource.LoadParams<K>, value: PagingSource.LoadResult.Data<Id, K, V, E>) {
        normalizedCache.normalize(key, value.items)
        delegate.put(key, value)
    }

    override fun putAll(map: Map<PagingSource.LoadParams<K>, PagingSource.LoadResult.Data<Id, K, V, E>>) {
        map.entries.forEach { (key, value) ->
            put(key, value)
        }
    }

}
