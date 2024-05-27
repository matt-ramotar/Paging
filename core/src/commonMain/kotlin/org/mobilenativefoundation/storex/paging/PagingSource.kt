package org.mobilenativefoundation.storex.paging

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import org.mobilenativefoundation.store5.core.Identifiable

interface PagingSource<Id : Comparable<Id>, K : Any, V : Identifiable<Id>, E : Any> {
    suspend fun load(params: LoadParams<K>): LoadResult<Id, K, V, E>

    @Serializable
    data class LoadParams<K : Any>(
        val key: K,
        val strategy: Strategy,
        val direction: Direction
    ) {
        @Serializable
        sealed interface Strategy {
            @Serializable
            data class CacheFirst(val refresh: Boolean) : Strategy
            @Serializable
            data object SkipCache : Strategy
        }

        enum class Direction {
            Prepend,
            Append
        }
    }
    @Serializable
    sealed interface LoadResult<Id : Comparable<Id>, K : Any, V : Identifiable<Id>, E : Any> {
        @Serializable
        data class Data<Id : Comparable<Id>, K : Any, V : Identifiable<Id>, E : Any>(
            val items: List<V>,
            val prevKey: K?,
            val params: LoadParams<K>,
            val nextKey: K?,
            val origin: Origin,
            val extras: JsonObject? = null
        ) : LoadResult<Id, K, V, E> {
            @Serializable
            enum class Origin {
                Network,
                MemoryCache,
                SourceOfTruth
            }
        }

        @Serializable
        data class Error<Id : Comparable<Id>, K : Any, V : Identifiable<Id>, E : Any>(
            val error: E,
            val extras: JsonObject? = null
        ) : LoadResult<Id, K, V, E>
    }
}