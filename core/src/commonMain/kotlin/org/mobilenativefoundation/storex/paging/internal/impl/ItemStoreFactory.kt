package org.mobilenativefoundation.storex.paging.internal.impl

import app.cash.sqldelight.coroutines.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.mobilenativefoundation.store.store5.Converter
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.SourceOfTruth
import org.mobilenativefoundation.store.store5.Store
import org.mobilenativefoundation.store.store5.StoreBuilder
import org.mobilenativefoundation.store5.cache.NormalizedCache
import org.mobilenativefoundation.store5.core.Identifiable
import org.mobilenativefoundation.storex.paging.Item
import org.mobilenativefoundation.storex.paging.PagingDb
import org.mobilenativefoundation.storex.paging.db.DriverFactory


@OptIn(InternalSerializationApi::class)
class ItemStoreFactory<Id : Comparable<Id>, K : Any, V : Identifiable<Id>>(
    private val fetcher: Fetcher<Id, V>,
    private val driverFactory: DriverFactory,
    private val registry: KClassRegistry<Id, K, V, *>,
    private val normalizedCache: NormalizedCache<Id, K, V>
) {

    fun create(): Store<Id, V> {
        return StoreBuilder.from(
            fetcher = fetcher,
            sourceOfTruth = createSourceOfTruth(),
            memoryCache = normalizedCache,
            converter = createConverter()
        ).build()
    }

    private fun createSourceOfTruth(): SourceOfTruth<Id, Item, V> {
        val db = PagingDb(driverFactory.createDriver())

        return SourceOfTruth.of(
            reader = { id ->
                db.itemQueries.getItem(Json.encodeToString(registry.id.serializer(), id)).asFlow()
                    .map { query ->
                        val item = query.executeAsOne()
                        Json.decodeFromString(registry.value.serializer(), item.data_)
                    }

            },
            writer = { _, item ->
                db.itemQueries.setItem(item)
            }
        )
    }

    private fun createConverter(): Converter<V, Item, V> {
        return Converter.Builder<V, Item, V>()
            .fromNetworkToLocal { network ->
                Item(
                    Json.encodeToString(registry.id.serializer(), network.id),
                    Json.encodeToString(registry.value.serializer(), network)
                )
            }

            .fromOutputToLocal { output ->
                Item(
                    Json.encodeToString(registry.id.serializer(), output.id),
                    Json.encodeToString(registry.value.serializer(), output)
                )
            }
            .build()
    }

}