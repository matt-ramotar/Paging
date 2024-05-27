package org.mobilenativefoundation.storex.paging.internal.impl

import app.cash.sqldelight.coroutines.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.serializer
import org.mobilenativefoundation.store.cache5.Cache
import org.mobilenativefoundation.store.cache5.CacheBuilder
import org.mobilenativefoundation.store.store5.Converter
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.SourceOfTruth
import org.mobilenativefoundation.store.store5.Store
import org.mobilenativefoundation.store.store5.StoreBuilder
import org.mobilenativefoundation.store5.cache.NormalizedCache
import org.mobilenativefoundation.store5.cache.NormalizingCache
import org.mobilenativefoundation.store5.core.Identifiable
import org.mobilenativefoundation.storex.paging.Item
import org.mobilenativefoundation.storex.paging.Page
import org.mobilenativefoundation.storex.paging.PagingDb
import org.mobilenativefoundation.storex.paging.PagingSource
import org.mobilenativefoundation.storex.paging.db.DriverFactory

@OptIn(InternalSerializationApi::class)
class PageStoreFactory<Id : Comparable<Id>, K : Any, V : Identifiable<Id>, E : Any>(
    private val fetcher: Fetcher<PagingSource.LoadParams<K>, PagingSource.LoadResult.Data<Id, K, V, E>>,
    private val driverFactory: DriverFactory,
    private val registry: KClassRegistry<Id, K, V, E>,
    private val normalizedCache: NormalizedCache<Id, K, V>,
) {

    private val db = PagingDb(driverFactory.createDriver())

    fun create(): Store<PagingSource.LoadParams<K>, PagingSource.LoadResult.Data<Id, K, V, E>> {
        return StoreBuilder.from(
            fetcher = fetcher,
            sourceOfTruth = createSourceOfTruth(),
            memoryCache = createMemoryCache(),
            converter = createConverter()
        ).build()
    }

    private fun createSourceOfTruth(): SourceOfTruth<PagingSource.LoadParams<K>, Page, PagingSource.LoadResult.Data<Id, K, V, E>> {

        return SourceOfTruth.of(
            reader = { params ->

                val encodedParams = Json.encodeToString(
                    PagingSource.LoadParams.serializer(registry.key.serializer()),
                    params
                )

                db.pageQueries.getPage(encodedParams).asFlow()
                    .map { query ->
                        val page = query.executeAsOne()

                        val encodedItemIds =
                            db.pageQueries.getPageItemIds(encodedParams).executeAsOne()

                        val itemIds: List<Id> = Json.decodeFromString(
                            ListSerializer(registry.id.serializer()),
                            encodedItemIds
                        )

                        val encodedItemIdsList = itemIds.map {
                            Json.encodeToString(registry.id.serializer(), it)
                        }

                        val items =
                            db.itemQueries.getItems(encodedItemIdsList).executeAsList().map {
                                Json.decodeFromString(registry.value.serializer(), it.data_)
                            }


                        val prevKey =
                            page.prevKey?.let {
                                Json.decodeFromString(
                                    registry.key.serializer(),
                                    it
                                )
                            }

                        val nextKey =
                            page.nextKey?.let {
                                Json.decodeFromString(
                                    registry.key.serializer(),
                                    it
                                )
                            }

                        val extras = page.extras?.let { Json.decodeFromString<JsonObject>(it) }

                        PagingSource.LoadResult.Data<Id, K, V, E>(
                            items = items,
                            prevKey = prevKey,
                            params = params,
                            nextKey = nextKey,
                            origin = PagingSource.LoadResult.Data.Origin.SourceOfTruth,
                            extras = extras
                        )
                    }
            },
            writer = { _, page ->
                db.pageQueries.setPage(page)
                // TODO(): Write items!


            }
        )
    }

    private fun createMemoryCache(): Cache<PagingSource.LoadParams<K>, PagingSource.LoadResult.Data<Id, K, V, E>> {
        val delegateCache =
            CacheBuilder<PagingSource.LoadParams<K>, PagingSource.LoadResult.Data<Id, K, V, E>>().build()
        return NormalizingCache(normalizedCache, delegateCache)
    }

    private fun createConverter(): Converter<PagingSource.LoadResult.Data<Id, K, V, E>, Page, PagingSource.LoadResult.Data<Id, K, V, E>> {
        return Converter.Builder<PagingSource.LoadResult.Data<Id, K, V, E>, Page, PagingSource.LoadResult.Data<Id, K, V, E>>()
            .fromNetworkToLocal { network ->

                val params = Json.encodeToString(
                    PagingSource.LoadParams.serializer(registry.key.serializer()),
                    network.params
                )

                // TODO(): Ideally we can fix this at the Store level

                network.items.forEach { item ->
                    db.itemQueries.setItem(
                        Item(
                            Json.encodeToString(registry.id.serializer(), item.id),
                            Json.encodeToString(registry.value.serializer(), item),
                            params
                        )
                    )
                }


                Page(
                    params = params,

                    nextKey = network.nextKey?.let {
                        Json.encodeToString(
                            registry.key.serializer(),
                            it
                        )
                    },
                    prevKey = network.prevKey?.let {
                        Json.encodeToString(
                            registry.key.serializer(),
                            it
                        )
                    },
                    extras = network.extras?.toString()
                )
            }

            .fromOutputToLocal { output ->
                Page(
                    params = Json.encodeToString(
                        PagingSource.LoadParams.serializer(registry.key.serializer()),
                        output.params
                    ),

                    nextKey = output.nextKey?.let {
                        Json.encodeToString(
                            registry.key.serializer(),
                            it
                        )
                    },
                    prevKey = output.prevKey?.let {
                        Json.encodeToString(
                            registry.key.serializer(),
                            it
                        )
                    },
                    extras = output.extras?.toString()
                )
            }
            .build()
    }
}