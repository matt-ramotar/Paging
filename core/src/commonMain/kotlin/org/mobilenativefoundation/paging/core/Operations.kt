package org.mobilenativefoundation.paging.core


/**
 * A strategy for sorting the paged data based on specific sorting parameters.
 *
 * @param Id The type of the unique identifier for each item in the paged data.
 * @param K The type of the key used for paging.
 * @param P The type of the parameters associated with each page of data.
 * @param D The type of the data items.
 * @param E The type of errors that can occur during the paging process.
 * @param S The type of the sorting parameters.
 * @param F The type of the filtering parameters.
 */
interface SortingStrategy<Id : Comparable<Id>, K : Any, P : Any, D : Any, E : Any, S : Any, F : Any> {
    /**
     * Sorts the paged data based on the provided sorting parameters.
     *
     * @param data The list of paged data items to be sorted.
     * @param sortingParams The sorting parameters to be applied.
     * @return The sorted list of paged data items.
     */
    fun sort(data: List<PagingData.Single<Id, K, P, D>>, sortingParams: S): List<PagingData.Single<Id, K, P, D>>
}

/**
 * A strategy for filtering the paged data based on specific filtering parameters.
 *
 * @param Id The type of the unique identifier for each item in the paged data.
 * @param K The type of the key used for paging.
 * @param P The type of the parameters associated with each page of data.
 * @param D The type of the data items.
 * @param E The type of errors that can occur during the paging process.
 * @param S The type of the sorting parameters.
 * @param F The type of the filtering parameters.
 */
interface FilteringStrategy<Id : Comparable<Id>, K : Any, P : Any, D : Any, E : Any, S : Any, F : Any> {
    /**
     * Filters the paged data based on the provided filtering parameters.
     *
     * @param data The list of paged data items to be filtered.
     * @param filterParams The filtering parameters to be applied.
     * @return The filtered list of paged data items.
     */
    fun filter(data: List<PagingData.Single<Id, K, P, D>>, filterParams: F): List<PagingData.Single<Id, K, P, D>>
}

/**
 * A strategy for grouping the paged data based on a specific criteria or field.
 *
 * @param Id The type of the unique identifier for each item in the paged data.
 * @param K The type of the key used for paging.
 * @param P The type of the parameters associated with each page of data.
 * @param D The type of the data items.
 * @param E The type of errors that can occur during the paging process.
 * @param S The type of the sorting parameters.
 * @param F The type of the filtering parameters.
 * @param G The type of the group key.
 */
interface GroupingStrategy<Id : Comparable<Id>, K : Any, P : Any, D : Any, E : Any, S : Any, F : Any, G : Any> {
    /**
     * Groups the paged data based on a specific criteria or field.
     *
     * @param data The list of paged data items to be grouped.
     * @return A map of grouped paged data items, where the key represents the group and the value is the list of items in that group.
     */
    fun groupBy(data: List<PagingData.Single<Id, K, P, D>>): Map<G, List<PagingData.Single<Id, K, P, D>>>
}

/**
 * A strategy for applying custom transformations or mappings to the paged data.
 *
 * @param Id The type of the unique identifier for each item in the paged data.
 * @param K The type of the key used for paging.
 * @param P The type of the parameters associated with each page of data.
 * @param D The type of the input data items.
 * @param E The type of errors that can occur during the paging process.
 * @param S The type of the sorting parameters.
 * @param F The type of the filtering parameters.
 * @param R The type of the transformed data items.
 */
interface TransformationStrategy<Id : Comparable<Id>, K : Any, P : Any, D : Any, E : Any, S : Any, F : Any, R : Any> {
    /**
     * Applies custom transformations or mappings to the paged data.
     *
     * @param data The list of paged data items to be transformed.
     * @return The transformed list of paged data items.
     */
    fun transform(data: List<PagingData.Single<Id, K, P, D>>): List<PagingData.Single<Id, K, P, R>>
}

/**
 * A strategy for removing duplicate items from the paged data based on a specific criteria.
 *
 * @param Id The type of the unique identifier for each item in the paged data.
 * @param K The type of the key used for paging.
 * @param P The type of the parameters associated with each page of data.
 * @param D The type of the data items.
 * @param E The type of errors that can occur during the paging process.
 * @param S The type of the sorting parameters.
 * @param F The type of the filtering parameters.
 */
interface DeduplicationStrategy<Id : Comparable<Id>, K : Any, P : Any, D : Any, E : Any, S : Any, F : Any> {
    /**
     * Removes duplicate items from the paged data based on a specific criteria.
     *
     * @param data The list of paged data items to be deduplicated.
     * @return The deduplicated list of paged data items.
     */
    fun deduplicate(data: List<PagingData.Single<Id, K, P, D>>): List<PagingData.Single<Id, K, P, D>>
}

/**
 * A strategy for caching the paged data to improve performance and reduce network requests.
 *
 * @param Id The type of the unique identifier for each item in the paged data.
 * @param K The type of the key used for paging.
 * @param P The type of the parameters associated with each page of data.
 * @param D The type of the data items.
 * @param E The type of errors that can occur during the paging process.
 * @param S The type of the sorting parameters.
 * @param F The type of the filtering parameters.
 */
interface CachingStrategy<Id : Comparable<Id>, K : Any, P : Any, D : Any, E : Any, S : Any, F : Any> {
    /**
     * Retrieves the cached paged data for the specified key.
     *
     * @param key The key associated with the cached paged data.
     * @return The cached list of paged data items, or null if not found in the cache.
     */
    suspend fun getFromCache(key: K): List<PagingData.Single<Id, K, P, D>>?

    /**
     * Saves the paged data to the cache for the specified key.
     *
     * @param key The key associated with the paged data to be cached.
     * @param data The list of paged data items to be cached.
     */
    suspend fun saveToCache(key: K, data: List<PagingData.Single<Id, K, P, D>>)
}

/**
 * A strategy for validating the paged data against specific rules or constraints.
 *
 * @param Id The type of the unique identifier for each item in the paged data.
 * @param K The type of the key used for paging.
 * @param P The type of the parameters associated with each page of data.
 * @param D The type of the data items.
 * @param E The type of errors that can occur during the paging process.
 * @param S The type of the sorting parameters.
 * @param F The type of the filtering parameters.
 */
interface ValidationStrategy<Id : Comparable<Id>, K : Any, P : Any, D : Any, E : Any, S : Any, F : Any> {
    /**
     * Validates the paged data against specific rules or constraints.
     *
     * @param data The list of paged data items to be validated.
     * @return The validated list of paged data items.
     */
    fun validate(data: List<PagingData.Single<Id, K, P, D>>): List<PagingData.Single<Id, K, P, D>>
}

/**
 * A strategy for customizing the pagination behavior, such as determining the next page key or handling end of pagination.
 *
 * @param Id The type of the unique identifier for each item in the paged data.
 * @param K The type of the key used for paging.
 * @param P The type of the parameters associated with each page of data.
 * @param D The type of the data items.
 * @param E The type of errors that can occur during the paging process.
 * @param S The type of the sorting parameters.
 * @param F The type of the filtering parameters.
 */
interface PaginationStrategy<Id : Comparable<Id>, K : Any, P : Any, D : Any, E : Any, S : Any, F : Any> {
    /**
     * Determines the next page key based on the current key and the paged data.
     *
     * @param currentKey The current page key.
     * @param data The list of paged data items.
     * @return The next page key, or null if there are no more pages.
     */
    fun getNextKey(currentKey: K, data: List<PagingData.Single<Id, K, P, D>>): K?

    /**
     * Checks if the pagination has reached the end based on the current key and the paged data.
     *
     * @param currentKey The current page key.
     * @param data The list of paged data items.
     * @return True if the pagination has reached the end, false otherwise.
     */
    fun isEndOfPagination(currentKey: K, data: List<PagingData.Single<Id, K, P, D>>): Boolean
}

/**
 * A strategy for merging multiple pages of data into a single list based on a specific criteria.
 *
 * @param Id The type of the unique identifier for each item in the paged data.
 * @param K The type of the key used for paging.
 * @param P The type of the parameters associated with each page of data.
 * @param D The type of the data items.
 * @param E The type of errors that can occur during the paging process.
 * @param S The type of the sorting parameters.
 * @param F The type of the filtering parameters.
 */
interface MergingStrategy<Id : Comparable<Id>, K : Any, P : Any, D : Any, E : Any, S : Any, F : Any> {
    /**
     * Merges multiple pages of data into a single list based on a specific criteria.
     *
     * @param pages The list of pages to be merged.
     * @return The merged list of paged data items.
     */
    fun merge(pages: List<List<D>>): List<PagingData.Single<Id, K, P, D>>
}

/**
 * A strategy for applying backend operations to the paging key and parameters.
 *
 * @param K The type of the key used for paging.
 * @param P The type of the parameters associated with each page of data.
 */
interface BackendOperations<K : Any, P : Any> {
    /**
     * Applies backend operations to the paging key and parameters.
     *
     * @param key The paging key.
     * @param params The paging parameters.
     * @return A pair of the modified paging key and parameters after applying the backend operations.
     */
    fun applyBackendOperations(key: K, params: P): Pair<K, P>
}