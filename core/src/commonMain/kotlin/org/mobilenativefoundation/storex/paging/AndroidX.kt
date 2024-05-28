package org.mobilenativefoundation.storex.paging

import org.mobilenativefoundation.store5.core.Identifiable

fun <Id : Comparable<Id>, K : Any, V : Identifiable<Id>, E : Any>
        androidx.paging.PagingSource.LoadParams<K>.storex(strategy: PagingSource.LoadParams.Strategy): PagingSource.LoadParams<K> {
    val direction = when (this) {
        is androidx.paging.PagingSource.LoadParams.Refresh -> {
            // Refresh is always Append
            PagingSource.LoadParams.Direction.Append
        }

        is androidx.paging.PagingSource.LoadParams.Prepend -> PagingSource.LoadParams.Direction.Prepend
        is androidx.paging.PagingSource.LoadParams.Append -> PagingSource.LoadParams.Direction.Append
    }

    return PagingSource.LoadParams(
        key = key ?: throw IllegalArgumentException("Key must not be null"),
        strategy = strategy,
        direction = direction
    )
}

fun <K : Any> PagingSource.LoadParams<K>.androidx(
    loadSize: Int,
    placeholdersEnabled: Boolean
): androidx.paging.PagingSource.LoadParams<K> {
    return when (direction) {
        PagingSource.LoadParams.Direction.Prepend -> {
            androidx.paging.PagingSource.LoadParams.Prepend(
                key = key,
                loadSize = loadSize,
                placeholdersEnabled = placeholdersEnabled
            )
        }

        PagingSource.LoadParams.Direction.Append -> {
            androidx.paging.PagingSource.LoadParams.Append(
                key = key,
                loadSize = loadSize,
                placeholdersEnabled = placeholdersEnabled
            )
        }
    }
}

fun <Id : Comparable<Id>, K : Any, V : Identifiable<Id>, E : Any> androidx.paging.PagingSource.LoadResult.Page<K, V>.storex(
    params: PagingSource.LoadParams<K>,
): PagingSource.LoadResult.Data<Id, K, V, E> {
    return PagingSource.LoadResult.Data(
        items = data,
        prevKey = prevKey,
        nextKey = nextKey,
        params = params,
        origin = PagingSource.LoadResult.Data.Origin.Network,
        itemsBefore = itemsBefore,
        itemsAfter = itemsAfter
    )
}

