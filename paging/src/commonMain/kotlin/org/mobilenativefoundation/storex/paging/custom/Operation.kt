package org.mobilenativefoundation.storex.paging.custom

import org.mobilenativefoundation.storex.paging.Identifiable
import org.mobilenativefoundation.storex.paging.PagingState
import org.mobilenativefoundation.storex.paging.Quantifiable
import org.mobilenativefoundation.storex.paging.internal.api.FetchingState

sealed interface Operation<Id : Comparable<Id>, Q : Quantifiable<Id>, K : Any, V : Identifiable<Id, Q>> {

    val strategy: TransformationStrategy<Id, Q, V>
    val shouldApply: (key: K?, pagingState: PagingState<Id, Q, *>, fetchingState: FetchingState<Id, Q, K>) -> Boolean

    data class Sort<Id : Comparable<Id>, Q : Quantifiable<Id>, K : Any, V : Identifiable<Id, Q>>(
        override val strategy: SortingStrategy<Id, Q, V>,
        override val shouldApply: (key: K?, pagingState: PagingState<Id, Q, *>, fetchingState: FetchingState<Id, Q, K>) -> Boolean
    ) : Operation<Id, Q, K, V>

    data class Filter<Id : Comparable<Id>, Q : Quantifiable<Id>, K : Any, V : Identifiable<Id, Q>>(
        override val strategy: FilteringStrategy<Id, Q, V>,
        override val shouldApply: (key: K?, pagingState: PagingState<Id, Q, *>, fetchingState: FetchingState<Id, Q, K>) -> Boolean
    ) : Operation<Id, Q, K, V>

    data class Group<Id : Comparable<Id>, Q : Quantifiable<Id>, K : Any, V : Identifiable<Id, Q>>(
        override val strategy: GroupingStrategy<Id, Q, V>,
        override val shouldApply: (key: K?, pagingState: PagingState<Id, Q, *>, fetchingState: FetchingState<Id, Q, K>) -> Boolean
    ) : Operation<Id, Q, K, V>


    data class Deduplicate<Id : Comparable<Id>, Q : Quantifiable<Id>, K : Any, V : Identifiable<Id, Q>>(
        override val strategy: DeduplicationStrategy<Id, Q, V>,
        override val shouldApply: (key: K?, pagingState: PagingState<Id, Q, *>, fetchingState: FetchingState<Id, Q, K>) -> Boolean
    ) : Operation<Id, Q, K, V>

    data class Validate<Id : Comparable<Id>, Q : Quantifiable<Id>, K : Any, V : Identifiable<Id, Q>>(
        override val strategy: ValidationStrategy<Id, Q, V>,
        override val shouldApply: (key: K?, pagingState: PagingState<Id, Q, *>, fetchingState: FetchingState<Id, Q, K>) -> Boolean
    ) : Operation<Id, Q, K, V>

}
