package org.mobilenativefoundation.storex.paging

import org.mobilenativefoundation.storex.paging.custom.ErrorHandlingStrategy

data class PagingConfig<Id: Comparable<Id>, K: Any>(
    val placeholderId: Id?,
    val initialKey: K,
    val pageSize: Int = DEFAULT_PAGE_SIZE,
    val prefetchDistance: Int = pageSize * DEFAULT_PREFETCH_DISTANCE_MULTIPLIER,
    val maxSize: Int = MAX_SIZE_UNBOUNDED,
    val jumpThreshold: Int = COUNT_UNDEFINED,
    val errorHandlingStrategy: ErrorHandlingStrategy = ErrorHandlingStrategy.RetryLast(),
) {
    companion object {
        const val MAX_SIZE_UNBOUNDED: Int = Int.MAX_VALUE
        internal const val COUNT_UNDEFINED = Int.MIN_VALUE
        const val DEFAULT_PAGE_SIZE = 10
        const val DEFAULT_PREFETCH_DISTANCE_MULTIPLIER = 2
    }
}