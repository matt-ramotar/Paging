package org.mobilenativefoundation.storex.paging.custom

import org.mobilenativefoundation.store5.core.Identifiable

sealed interface Operation<Id : Comparable<Id>, K : Any, V : Identifiable<Id>, P : Any> {

    val strategy: TransformationStrategy<Id, V, P>
    val shouldApply: (key: K?) -> Boolean

    data class Sort<Id : Comparable<Id>, K : Any, V : Identifiable<Id>, P : Any>(
        override val strategy: SortingStrategy<Id, V, P>,
        override val shouldApply: (key: K?) -> Boolean
    ) : Operation<Id, K, V, P>

    data class Filter<Id : Comparable<Id>, K : Any, V : Identifiable<Id>, P : Any>(
        override val strategy: FilteringStrategy<Id, V, P>,
        override val shouldApply: (key: K?) -> Boolean
    ) : Operation<Id, K, V, P>

    data class Group<Id : Comparable<Id>, K : Any, V : Identifiable<Id>, P : Any>(
        override val strategy: GroupingStrategy<Id, V, P>,
        override val shouldApply: (key: K?) -> Boolean
    ) : Operation<Id, K, V, P>


    data class Deduplicate<Id : Comparable<Id>, K : Any, V : Identifiable<Id>, P : Any>(
        override val strategy: DeduplicationStrategy<Id, V, P>,
        override val shouldApply: (key: K?) -> Boolean,
    ) : Operation<Id, K, V, P>

    data class Validate<Id : Comparable<Id>, K : Any, V : Identifiable<Id>, P : Any>(
        override val strategy: ValidationStrategy<Id, V, P>,
        override val shouldApply: (key: K?) -> Boolean,
    ) : Operation<Id, K, V, P>

}
