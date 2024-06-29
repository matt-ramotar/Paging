package org.mobilenativefoundation.storex.paging.scope

import app.cash.sqldelight.coroutines.asFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.mobilenativefoundation.storex.paging.*
import org.mobilenativefoundation.storex.paging.db.DriverFactory
import org.mobilenativefoundation.storex.paging.internal.impl.KClassRegistry

interface Database<Id : Identifier<Id>, K : Comparable<K>, V : Identifiable<Id>> {
    val itemQueries: ItemQueries<Id, K, V>
    val pageQueries: PageQueries<Id, K, V>

    interface ItemQueries<Id : Identifier<Id>, K : Comparable<K>, V : Identifiable<Id>> {
        fun streamItem(id: Id): Flow<V?>
        fun getItem(id: Id): V?

        fun setItem(id: Id, data: V, params: PagingSource.LoadParams<K>)

        fun updateItem(id: Id, data: V)

        fun removeItem(id: Id)

        fun removeAllItems()
    }

    interface PageQueries<Id : Identifier<Id>, K : Comparable<K>, V : Identifiable<Id>> {
        fun exists(params: PagingSource.LoadParams<K>): Boolean

        fun setPage(params: PagingSource.LoadParams<K>, data: PagingSource.LoadResult.Data<Id, K, V>)

        fun removePage(params: PagingSource.LoadParams<K>)

        fun removeAllPages()
    }
}


class RealDatabase<Id : Identifier<Id>, K : Comparable<K>, V : Identifiable<Id>>(
    driverFactory: DriverFactory,
    registry: KClassRegistry<Id, K, V>
) : Database<Id, K, V> {

    private val delegate = PagingDb(driverFactory.createDriver())

    override val itemQueries: Database.ItemQueries<Id, K, V> = RealItemQueries(delegate, registry)
    override val pageQueries: Database.PageQueries<Id, K, V> = RealPageQueries(delegate, registry)
}

@OptIn(InternalSerializationApi::class)
class RealItemQueries<Id : Identifier<Id>, K : Comparable<K>, V : Identifiable<Id>>(
    private val delegate: PagingDb,
    private val registry: KClassRegistry<Id, K, V>
) : Database.ItemQueries<Id, K, V> {
    override fun getItem(id: Id): V? {
        val encodedId = Json.encodeToString(registry.id.serializer(), id)
        val encodedItem = delegate.itemQueries.getItem(encodedId).executeAsOneOrNull()
        return encodedItem?.let { Json.decodeFromString(registry.value.serializer(), it.data_) }
    }

    override fun setItem(id: Id, data: V, params: PagingSource.LoadParams<K>) {
        val encodedId = Json.encodeToString(registry.id.serializer(), id)

        val encodedParams = Json.encodeToString(
            serializer = PagingSource.LoadParams.serializer(registry.key.serializer()),
            value = params
        )

        delegate.itemQueries.setItem(
            Item(
                encodedId,
                Json.encodeToString(registry.value.serializer(), data),
                encodedParams
            )
        )
    }

    override fun updateItem(id: Id, data: V) {
        val encodedId = Json.encodeToString(registry.id.serializer(), id)

        delegate.itemQueries.updateItem(
            Json.encodeToString(registry.value.serializer(), data),
            encodedId
        )
    }

    override fun removeItem(id: Id) {
        val encodedItemId = Json.encodeToString(registry.id.serializer(), id)
        delegate.itemQueries.removeItem(encodedItemId)

    }

    override fun removeAllItems() {
        delegate.itemQueries.removeAllItems()
    }

    override fun streamItem(id: Id): Flow<V?> {
        val encodedId = Json.encodeToString(registry.id.serializer(), id)

        return delegate.itemQueries.getItem(encodedId).asFlow().map { query ->
            val item = query.executeAsOneOrNull()
            if (item != null) {
                Json.decodeFromString(registry.value.serializer(), item.data_)
            } else {
                null
            }
        }
    }

}

@OptIn(InternalSerializationApi::class)
class RealPageQueries<Id : Identifier<Id>, K : Comparable<K>, V : Identifiable<Id>>(
    private val delegate: PagingDb,
    private val registry: KClassRegistry<Id, K, V>
) : Database.PageQueries<Id, K, V> {
    override fun exists(params: PagingSource.LoadParams<K>): Boolean {
        val encodedParams = Json.encodeToString(
            PagingSource.LoadParams.serializer(registry.key.serializer()),
            params
        )

        return delegate.pageQueries.getPage(encodedParams).executeAsOneOrNull() != null
    }

    override fun setPage(params: PagingSource.LoadParams<K>, data: PagingSource.LoadResult.Data<Id, K, V>) {
        val encodedParams = Json.encodeToString(
            PagingSource.LoadParams.serializer(registry.key.serializer()),
            params
        )

        val page = Page(
            params = encodedParams,
            nextKey = data.nextKey?.let { key ->
                Json.encodeToString(registry.key.serializer(), key)
            },
            prevKey = data.prevKey?.let { key ->
                Json.encodeToString(registry.key.serializer(), key)
            },
            extras = data.extras?.toString()
        )

        delegate.pageQueries.setPage(page)
    }

    override fun removePage(params: PagingSource.LoadParams<K>) {
        val encodedParams = Json.encodeToString(
            PagingSource.LoadParams.serializer(registry.key.serializer()),
            params
        )

        delegate.pageQueries.removePage(encodedParams)
    }

    override fun removeAllPages() {
        delegate.pageQueries.removeAllPages()
    }

}