package org.mobilenativefoundation.storex.paging

import org.mobilenativefoundation.storex.paging.custom.ErrorFactory
import org.mobilenativefoundation.storex.paging.custom.Operation
import org.mobilenativefoundation.storex.paging.db.DriverFactory

fun <Id : Comparable<Id>, K : Any, V : Identifiable<Id>, E : Any>
        androidx.paging.PagingSource.LoadParams<K>.storex(strategy: LoadStrategy): PagingSource.LoadParams<K> {
    val direction = when (this) {
        is androidx.paging.PagingSource.LoadParams.Refresh -> {
            // Refresh is always Append
            LoadDirection.Append
        }

        is androidx.paging.PagingSource.LoadParams.Prepend -> LoadDirection.Prepend
        is androidx.paging.PagingSource.LoadParams.Append -> LoadDirection.Append
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
        LoadDirection.Prepend -> {
            androidx.paging.PagingSource.LoadParams.Prepend(
                key = key,
                loadSize = loadSize,
                placeholdersEnabled = placeholdersEnabled
            )
        }

        LoadDirection.Append -> {
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


inline fun <reified Id : Comparable<Id>, reified K : Any, reified V : Identifiable<Id>> androidx.paging.PagingSource<K, V>.storex(
    pagingConfig: PagingConfig<Id, K>,
): Pager<Id, K, V, Throwable> {
    return Pager.Builder<Id, K, V>(
        pagingConfig = pagingConfig,
    ).androidxPagingSource(this).build()
}


inline fun <reified Id : Comparable<Id>, reified K : Any, reified V : Identifiable<Id>> androidx.paging.PagingSource<K, V>.storex(
    pagingConfig: PagingConfig<Id, K>,
    driverFactory: DriverFactory
): Pager<Id, K, V, Throwable> {
    return Pager.Builder<Id, K, V>(
        pagingConfig = pagingConfig,
        driverFactory = driverFactory
    ).androidxPagingSource(this).build()
}


inline fun <reified Id : Comparable<Id>, reified K : Any, reified V : Identifiable<Id>, reified E : Any> androidx.paging.PagingSource<K, V>.storex(
    pagingConfig: PagingConfig<Id, K>,
    driverFactory: DriverFactory,
    errorFactory: ErrorFactory<E>
): Pager<Id, K, V, E> {
    return Pager.Builder<Id, K, V, E>(
        pagingConfig = pagingConfig,
        driverFactory = driverFactory,
        errorFactory = errorFactory
    ).androidxPagingSource(this).build()
}


inline fun <reified Id : Comparable<Id>, reified K : Any, reified V : Identifiable<Id>, reified E : Any, reified P : Any> androidx.paging.PagingSource<K, V>.storex(
    pagingConfig: PagingConfig<Id, K>,
    driverFactory: DriverFactory,
    errorFactory: ErrorFactory<E>,
    operations: List<Operation<Id, K, V, P, P>>,
): Pager<Id, K, V, E> {
    return Pager.Builder<Id, K, V, E, P>(
        pagingConfig = pagingConfig,
        driverFactory = driverFactory,
        errorFactory = errorFactory,
        operations = operations
    ).androidxPagingSource(this).build()
}
