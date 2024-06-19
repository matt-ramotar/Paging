package org.mobilenativefoundation.storex.paging

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

fun interface PagingSource<Id : Identifier<*>, K : Any, V : Identifiable<Id>> {
    suspend fun load(params: LoadParams<K>): LoadResult<Id, K, V>

    @Serializable
    data class LoadParams<K : Any>(
        val key: K,
        val strategy: LoadStrategy,
        val direction: LoadDirection
    )

    @Serializable
    sealed interface LoadResult<Id : Identifier<*>, K : Any, V : Identifiable<Id>> {
        @Serializable
        data class Data<Id : Identifier<*>, K : Any, V : Identifiable<Id>>(
            val items: List<V>,
            val prevKey: K?,
            val params: LoadParams<K>,
            val nextKey: K?,
            val origin: Origin,
            val itemsBefore: Int? = null,
            val itemsAfter: Int? = null,
            val extras: JsonObject? = null
        ) : LoadResult<Id, K, V> {
            @Serializable
            enum class Origin {
                Network,
                MemoryCache,
                SourceOfTruth
            }

            val normalized: Normalized<Id, K>
                get() = Normalized(
                    items = items.map { it.id },
                    prevKey = prevKey,
                    params = params,
                    nextKey = nextKey,
                    origin = origin
                )

        }

        @Serializable
        sealed class Error<Id : Identifier<*>, K : Comparable<K>, V : Identifiable<Id>> : LoadResult<Id, K, V> {
            data class Exception<Id : Identifier<*>, K : Comparable<K>, V : Identifiable<Id>>(
                val error: Throwable,
                val extras: JsonObject? = null
            ) : Error<Id, K, V>()

            data class Message<Id : Identifier<*>, K : Comparable<K>, V : Identifiable<Id>>(
                val error: String,
                val extras: JsonObject? = null
            ) : Error<Id, K, V>()
        }

        data class Normalized<Id : Identifier<*>, K : Any>(
            val items: List<Id>,
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
    data class CacheFirst(val alsoLoadFromNetwork: Boolean) : LoadStrategy

    @Serializable
    data object SkipCache : LoadStrategy

    @Serializable
    data object LocalOnly : LoadStrategy
}