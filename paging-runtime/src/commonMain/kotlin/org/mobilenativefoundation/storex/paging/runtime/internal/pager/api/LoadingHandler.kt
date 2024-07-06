package org.mobilenativefoundation.storex.paging.runtime.internal.pager.api

import org.mobilenativefoundation.storex.paging.runtime.Identifiable
import org.mobilenativefoundation.storex.paging.runtime.Identifier
import org.mobilenativefoundation.storex.paging.runtime.PagingSource

/**
 * Handles loading operations for both append and prepend directions.
 */
internal interface LoadingHandler<Id : Identifier<Id>, K : Comparable<K>, V : Identifiable<Id>> {
    suspend fun handleAppendLoading(
        loadParams: PagingSource.LoadParams<K>,
        addNextToQueue: Boolean = true
    )

    suspend fun handlePrependLoading(loadParams: PagingSource.LoadParams<K>)
}