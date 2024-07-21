package org.mobilenativefoundation.storex.paging.runtime

import kotlinx.serialization.json.JsonObject

fun interface PagingSource<ItemId : Any, PageRequestKey : Any, ItemValue : Any> {
    suspend fun load(params: LoadParams<PageRequestKey>): LoadResult<ItemId, PageRequestKey, ItemValue>


    data class LoadParams<K : Any>(
        val key: K,
        val strategy: LoadStrategy,
        val direction: LoadDirection
    )


    sealed interface LoadResult<Id : Any, K : Any, V : Any> {

        data class Data<Id : Any, K : Any, V : Any>(
            val items: List<V>,
            val prevKey: K?,
            val params: LoadParams<K>,
            val nextKey: K?,
            val origin: Origin,
            val itemsBefore: Int? = null,
            val itemsAfter: Int? = null,
            val extras: JsonObject? = null
        ) : LoadResult<Id, K, V> {

            enum class Origin {
                Network,
                MemoryCache,
                SourceOfTruth,
                Placeholder
            }
        }


        sealed class Error<Id : Any, K : Any, V : Any> :
            LoadResult<Id, K, V> {
            data class Exception<Id : Any, K : Any, V : Any>(
                val error: Throwable,
                val extras: JsonObject? = null
            ) : Error<Id, K, V>()

            data class Message<Id : Any, K : Any, V : Any>(
                val error: String,
                val extras: JsonObject? = null
            ) : Error<Id, K, V>()
        }

        data class Normalized<Id : Any, K : Any>(
            val items: List<Id>,
            val prevKey: K?,
            val params: LoadParams<K>,
            val nextKey: K?,
            val origin: Data.Origin,
            val extras: JsonObject? = null
        )
    }
}
