package org.mobilenativefoundation.store5.cache

import org.mobilenativefoundation.store.cache5.Cache
import org.mobilenativefoundation.store5.core.Identifiable
import org.mobilenativefoundation.storex.paging.PagingSource

class RealNormalizedCache<Id : Comparable<Id>, K : Any, V : Identifiable<Id>>(
    private val delegate: Cache<Id, V>
) : NormalizedCache<Id, K, V> {

    private val keyToIds = mutableMapOf<PagingSource.LoadParams<K>, MutableList<Id>>()
    override fun getIfPresent(key: Id): V? = delegate.getIfPresent(key)

    override fun getOrPut(key: Id, valueProducer: () -> V): V =
        delegate.getOrPut(key, valueProducer)

    override fun normalize(key: PagingSource.LoadParams<K>, values: List<V>) {
        keyToIds.getOrPut(key) { mutableListOf() }.addAll(values.map { it.id })

        values.forEach {
            put(it.id, it)
        }
    }

    override fun getAllPresent(key: PagingSource.LoadParams<K>): List<Id> {
        return keyToIds[key] ?: emptyList()
    }

    override fun getAllPresent(keys: List<*>): Map<Id, V> = delegate.getAllPresent(keys)

    override fun invalidateAll() {
        delegate.invalidateAll()
    }

    override fun size(): Long = delegate.size()

    override fun invalidateAll(keys: List<Id>) {
        delegate.invalidateAll(keys)
    }

    override fun invalidate(key: Id) {
        delegate.invalidate(key)
    }

    override fun putAll(map: Map<Id, V>) {
        delegate.putAll(map)
    }

    override fun put(key: Id, value: V) {
        delegate.put(key, value)
    }


}
