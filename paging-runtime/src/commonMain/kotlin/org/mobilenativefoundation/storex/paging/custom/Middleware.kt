package org.mobilenativefoundation.storex.paging.custom

import org.mobilenativefoundation.storex.paging.runtime.PagingSource

/**
 * A functional interface representing middleware for processing load parameters.
 * Middleware can be used to modify or intercept load parameters before they are
 * used to fetch data.
 *
 * @param PageRequestKey The type of the paging key.
 */
fun interface Middleware<PageRequestKey : Any> {
    /**
     * Applies the middleware to the given load parameters.
     *
     * @param params The original load parameters.
     * @param next A function to call the next middleware in the chain or the final load operation.
     * @return The potentially modified load parameters.
     */
    suspend fun apply(
        params: PagingSource.LoadParams<PageRequestKey>,
        next: suspend (PagingSource.LoadParams<PageRequestKey>) -> PagingSource.LoadParams<PageRequestKey>
    ): PagingSource.LoadParams<PageRequestKey>
}