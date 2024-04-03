package org.mobilenativefoundation.paging.core.impl

import org.mobilenativefoundation.paging.core.AggregatingStrategy
import org.mobilenativefoundation.paging.core.DeduplicationStrategy
import org.mobilenativefoundation.paging.core.FilteringStrategy
import org.mobilenativefoundation.paging.core.GroupingStrategy
import org.mobilenativefoundation.paging.core.InsertionStrategy
import org.mobilenativefoundation.paging.core.PagingBuffer
import org.mobilenativefoundation.paging.core.PagingConfig
import org.mobilenativefoundation.paging.core.PagingData
import org.mobilenativefoundation.paging.core.PagingItems
import org.mobilenativefoundation.paging.core.PagingKey
import org.mobilenativefoundation.paging.core.PagingSource
import org.mobilenativefoundation.paging.core.SortingStrategy
import org.mobilenativefoundation.paging.core.ValidationStrategy

/**
 * A default implementation of [AggregatingStrategy] that supports complex operations such as sorting,
 * filtering, grouping, transformation, deduplication, and validation.
 *
 * @param Id The type of the unique identifier for each item in the paged data.
 * @param K The type of the key used for paging.
 * @param P The type of the parameters associated with each page of data.
 * @param S The type of the sorting parameters.
 * @param F The type of the filtering parameters.
 * @param D The type of the data items.
 */
class DefaultAggregatingStrategy<Id : Comparable<Id>, K : Any, P : Any, D : Any, E : Any, S : Any, F : Any> private constructor(
    private val operations: List<Operation<Id, K, P, D, E, S, F>>
) : AggregatingStrategy<Id, K, P, D> {

    override fun aggregate(
        anchorPosition: PagingKey<K, P>,
        prefetchPosition: PagingKey<K, P>?,
        pagingConfig: PagingConfig,
        pagingBuffer: PagingBuffer<Id, K, P, D>
    ): PagingItems<Id, K, P, D> {

        if (pagingBuffer.isEmpty()) return PagingItems(emptyList())

        var orderedItems = mutableListOf<PagingData.Single<Id, K, P, D>>()

        var currentPage: PagingSource.LoadResult.Data<Id, K, P, D>? = pagingBuffer.head()

        while (currentPage != null) {
            when (pagingConfig.insertionStrategy) {
                InsertionStrategy.PREPEND -> orderedItems.addAll(0, currentPage.collection.items)
                InsertionStrategy.APPEND -> orderedItems.addAll(currentPage.collection.items)
                InsertionStrategy.REPLACE -> {
                    orderedItems.clear()
                    orderedItems.addAll(currentPage.collection.items)
                }
            }

            currentPage = currentPage.collection.nextKey?.let { pagingBuffer.get(it) }
        }


        operations.forEach { operation ->
            if (operation.shouldApply(anchorPosition)) {
                val nextOrderedItems = when (operation) {
                    is Operation.Sorting -> operation.strategy.sort(orderedItems, operation.params)
                    is Operation.Filtering -> operation.strategy.filter(orderedItems, operation.params)
                    is Operation.Grouping -> operation.strategy.groupBy(orderedItems).values.flatten()
                    is Operation.Deduplication -> operation.strategy.deduplicate(orderedItems)
                    is Operation.Validation -> operation.strategy.validate(orderedItems)
                }

                orderedItems = nextOrderedItems.toMutableList()
            }
        }

        return PagingItems(data = orderedItems)
    }

    /**
     * A builder class for constructing instances of [DefaultAggregatingStrategy].
     *
     * @param Id The type of the unique identifier for each item in the paged data.
     * @param K The type of the key used for paging.
     * @param P The type of the parameters associated with each page of data.
     * @param S The type of the sorting parameters.
     * @param F The type of the filtering parameters.
     * @param D The type of the data items.
     */
    class Builder<Id : Comparable<Id>, K : Any, P : Any, D : Any, E : Any, S : Any, F : Any> {
        private val operations = mutableListOf<Operation<Id, K, P, D, E, S, F>>()

        /**
         * Adds a [SortingStrategy] operation to be applied during aggregation.
         *
         * @param strategy The sorting strategy to be used.
         * @param params The sorting parameters to be applied.
         * @return The [Builder] instance for chaining.
         */
        fun addSorting(strategy: SortingStrategy<Id, K, P, D, E, S, F>, params: S, shouldApply: (key: PagingKey<K, P>) -> Boolean) = apply {
            operations.add(Operation.Sorting(strategy, params, shouldApply))
        }

        /**
         * Adds a [FilteringStrategy] operation to be applied during aggregation.
         *
         * @param strategy The filtering strategy to be used.
         * @param params The filtering parameters to be applied.
         * @return The [Builder] instance for chaining.
         */
        fun addFiltering(strategy: FilteringStrategy<Id, K, P, D, E, S, F>, params: F, shouldApply: (key: PagingKey<K, P>) -> Boolean) =
            apply {
                operations.add(Operation.Filtering(strategy, params, shouldApply))
            }

        /**
         * Adds a [GroupingStrategy] operation to be applied during aggregation.
         *
         * @param strategy The grouping strategy to be used.
         * @return The [Builder] instance for chaining.
         */
        fun addGrouping(strategy: GroupingStrategy<Id, K, P, D, E, S, F, *>, shouldApply: (key: PagingKey<K, P>) -> Boolean) = apply {
            operations.add(Operation.Grouping(strategy, shouldApply))
        }

        /**
         * Adds a [DeduplicationStrategy] operation to be applied during aggregation.
         *
         * @param strategy The deduplication strategy to be used.
         * @return The [Builder] instance for chaining.
         */
        fun addDeduplication(strategy: DeduplicationStrategy<Id, K, P, D, E, S, F>, shouldApply: (key: PagingKey<K, P>) -> Boolean) =
            apply {
                operations.add(Operation.Deduplication(strategy, shouldApply))
            }

        /**
         * Adds a [ValidationStrategy] operation to be applied during aggregation.
         *
         * @param strategy The validation strategy to be used.
         * @return The [Builder] instance for chaining.
         */
        fun addValidation(strategy: ValidationStrategy<Id, K, P, D, E, S, F>, shouldApply: (key: PagingKey<K, P>) -> Boolean) = apply {
            operations.add(Operation.Validation(strategy, shouldApply))
        }

        /**
         * Builds and returns an instance of [DefaultAggregatingStrategy] with the configured operations.
         *
         * @return The constructed [DefaultAggregatingStrategy] instance.
         */
        fun build(): DefaultAggregatingStrategy<Id, K, P, D, E, S, F> {
            return DefaultAggregatingStrategy(operations)
        }
    }

    internal sealed class Operation<Id : Comparable<Id>, K : Any, P : Any, out D : Any, out E : Any, out S : Any, out F : Any> {

        abstract val shouldApply: (key: PagingKey<K, P>) -> Boolean

        data class Sorting<Id : Comparable<Id>, K : Any, P : Any, D : Any, E : Any, S : Any, F : Any>(
            val strategy: SortingStrategy<Id, K, P, D, E, S, F>,
            val params: S,
            override val shouldApply: (key: PagingKey<K, P>) -> Boolean
        ) : Operation<Id, K, P, D, E, S, F>()

        data class Filtering<Id : Comparable<Id>, K : Any, P : Any, D : Any, E : Any, S : Any, F : Any>(
            val strategy: FilteringStrategy<Id, K, P, D, E, S, F>,
            val params: F,
            override val shouldApply: (key: PagingKey<K, P>) -> Boolean
        ) : Operation<Id, K, P, D, E, S, F>()

        data class Grouping<Id : Comparable<Id>, K : Any, P : Any, D : Any, E : Any, S : Any, F : Any>(
            val strategy: GroupingStrategy<Id, K, P, D, E, S, F, *>,
            override val shouldApply: (key: PagingKey<K, P>) -> Boolean
        ) : Operation<Id, K, P, D, E, S, F>()

        data class Deduplication<Id : Comparable<Id>, K : Any, P : Any, D : Any, E : Any, S : Any, F : Any>(
            val strategy: DeduplicationStrategy<Id, K, P, D, E, S, F>,
            override val shouldApply: (key: PagingKey<K, P>) -> Boolean
        ) : Operation<Id, K, P, D, E, S, F>()

        data class Validation<Id : Comparable<Id>, K : Any, P : Any, D : Any, E : Any, S : Any, F : Any>(
            val strategy: ValidationStrategy<Id, K, P, D, E, S, F>,
            override val shouldApply: (key: PagingKey<K, P>) -> Boolean
        ) : Operation<Id, K, P, D, E, S, F>()
    }
}