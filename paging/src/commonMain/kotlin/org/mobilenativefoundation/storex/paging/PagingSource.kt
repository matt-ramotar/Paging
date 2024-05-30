package org.mobilenativefoundation.storex.paging

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

fun interface PagingSource<Id : Comparable<Id>, K : Any, V : Identifiable<Id>, E : Any> {
    suspend fun load(params: LoadParams<K>): LoadResult<Id, K, V, E>

    @Serializable
    data class LoadParams<K : Any>(
        val key: K,
        val strategy: LoadStrategy,
        val direction: LoadDirection
    )

    @Serializable
    sealed interface LoadResult<Id : Comparable<Id>, K : Any, V : Identifiable<Id>, E : Any> {
        @Serializable
        data class Data<Id : Comparable<Id>, K : Any, V : Identifiable<Id>, E : Any>(
            val items: List<V>,
            val prevKey: K?,
            val params: LoadParams<K>,
            val nextKey: K?,
            val origin: Origin,
            val itemsBefore: Int? = null,
            val itemsAfter: Int? = null,
            val extras: JsonObject? = null
        ) : LoadResult<Id, K, V, E> {
            @Serializable
            enum class Origin {
                Network,
                MemoryCache,
                SourceOfTruth
            }

            val normalized: Normalized<Id, K, V, E>
                get() = Normalized(
                    items = items.map { it.id },
                    prevKey = prevKey,
                    params = params,
                    nextKey = nextKey,
                    origin = origin
                )

        }

        @Serializable
        data class Error<Id : Comparable<Id>, K : Any, V : Identifiable<Id>, E : Any>(
            val error: E,
            val extras: JsonObject? = null
        ) : LoadResult<Id, K, V, E>

        data class Normalized<Id : Comparable<Id>, K : Any, V : Identifiable<Id>, E : Any>(
            val items: List<Quantifiable<Id>>,
            val prevKey: K?,
            val params: LoadParams<K>,
            val nextKey: K?,
            val origin: Data.Origin,
            val extras: JsonObject? = null
        )
    }
}

enum class LoadDirection {
    Prepend,
    Append
}

@Serializable
sealed interface LoadStrategy {
    @Serializable
    data class CacheFirst(val refresh: Boolean) : LoadStrategy

    @Serializable
    data object SkipCache : LoadStrategy

    @Serializable
    data object LocalOnly : LoadStrategy

    @Serializable
    data object Refresh : LoadStrategy
}