package org.mobilenativefoundation.storex.paging.custom

import org.mobilenativefoundation.store5.core.Identifiable
import org.mobilenativefoundation.storex.paging.PagingState
import org.mobilenativefoundation.storex.paging.internal.api.FetchingState

sealed interface Operation<Id : Comparable<Id>, K : Any, V : Identifiable<Id>, P : Any, T : P> {

    val strategy: TransformationStrategy<Id, V, T>
    val shouldApply: (key: K?, pagingState: PagingState<Id, *>, fetchingState: FetchingState<Id>) -> T?

    data class Sort<Id : Comparable<Id>, K : Any, V : Identifiable<Id>, P : Any, T : P>(
        override val strategy: SortingStrategy<Id, V, P, T>,
        override val shouldApply: (key: K?, pagingState: PagingState<Id, *>, fetchingState: FetchingState<Id>) -> T?
    ) : Operation<Id, K, V, P, T>

    data class Filter<Id : Comparable<Id>, K : Any, V : Identifiable<Id>, P : Any, T : P>(
        override val strategy: FilteringStrategy<Id, V, P, T>,
        override val shouldApply: (key: K?, pagingState: PagingState<Id, *>, fetchingState: FetchingState<Id>) -> T?
    ) : Operation<Id, K, V, P, T>

    data class Group<Id : Comparable<Id>, K : Any, V : Identifiable<Id>, P : Any, T : P>(
        override val strategy: GroupingStrategy<Id, V, P, T>,
        override val shouldApply: (key: K?, pagingState: PagingState<Id, *>, fetchingState: FetchingState<Id>) -> T?
    ) : Operation<Id, K, V, P, T>


    data class Deduplicate<Id : Comparable<Id>, K : Any, V : Identifiable<Id>, P : Any, T : P>(
        override val strategy: DeduplicationStrategy<Id, V, P, T>,
        override val shouldApply: (key: K?, pagingState: PagingState<Id, *>, fetchingState: FetchingState<Id>) -> T?,
    ) : Operation<Id, K, V, P, T>

    data class Validate<Id : Comparable<Id>, K : Any, V : Identifiable<Id>, P : Any, T : P>(
        override val strategy: ValidationStrategy<Id, V, P, T>,
        override val shouldApply: (key: K?, pagingState: PagingState<Id, *>, fetchingState: FetchingState<Id>) -> T?,
    ) : Operation<Id, K, V, P, T>

}
