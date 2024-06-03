package org.mobilenativefoundation.storex.paging.custom

import org.mobilenativefoundation.storex.paging.Identifiable
import org.mobilenativefoundation.storex.paging.PagingState
import org.mobilenativefoundation.storex.paging.Quantifiable
import org.mobilenativefoundation.storex.paging.internal.api.FetchingState

sealed interface Operation<Id : Comparable<Id>, Q : Quantifiable<Id>, K : Any, V : Identifiable<Id, Q>, P : Any, T : P> {

    val strategy: TransformationStrategy<Id, Q, V, T>
    val shouldApply: (key: K?, pagingState: PagingState<Id, Q, *>, fetchingState: FetchingState<Id, Q, K>) -> T?

    data class Sort<Id : Comparable<Id>, Q : Quantifiable<Id>, K : Any, V : Identifiable<Id, Q>, P : Any, T : P>(
        override val strategy: SortingStrategy<Id, Q, V, P, T>,
        override val shouldApply: (key: K?, pagingState: PagingState<Id, Q, *>, fetchingState: FetchingState<Id, Q, K>) -> T?
    ) : Operation<Id, Q, K, V, P, T>

    data class Filter<Id : Comparable<Id>, Q : Quantifiable<Id>, K : Any, V : Identifiable<Id, Q>, P : Any, T : P>(
        override val strategy: FilteringStrategy<Id, Q, V, P, T>,
        override val shouldApply: (key: K?, pagingState: PagingState<Id, Q, *>, fetchingState: FetchingState<Id, Q, K>) -> T?
    ) : Operation<Id, Q, K, V, P, T>

    data class Group<Id : Comparable<Id>, Q : Quantifiable<Id>, K : Any, V : Identifiable<Id, Q>, P : Any, T : P>(
        override val strategy: GroupingStrategy<Id, Q, V, P, T>,
        override val shouldApply: (key: K?, pagingState: PagingState<Id, Q, *>, fetchingState: FetchingState<Id, Q, K>) -> T?
    ) : Operation<Id, Q, K, V, P, T>


    data class Deduplicate<Id : Comparable<Id>, Q : Quantifiable<Id>, K : Any, V : Identifiable<Id, Q>, P : Any, T : P>(
        override val strategy: DeduplicationStrategy<Id, Q, V, P, T>,
        override val shouldApply: (key: K?, pagingState: PagingState<Id, Q, *>, fetchingState: FetchingState<Id, Q, K>) -> T?,
    ) : Operation<Id, Q, K, V, P, T>

    data class Validate<Id : Comparable<Id>, Q : Quantifiable<Id>, K : Any, V : Identifiable<Id, Q>, P : Any, T : P>(
        override val strategy: ValidationStrategy<Id, Q, V, P, T>,
        override val shouldApply: (key: K?, pagingState: PagingState<Id, Q, *>, fetchingState: FetchingState<Id, Q, K>) -> T?,
    ) : Operation<Id, Q, K, V, P, T>

}
