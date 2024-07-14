package org.mobilenativefoundation.storex.paging.runtime.internal.pager.api

import org.mobilenativefoundation.storex.paging.runtime.Identifier

interface ListSortAnalyzer<Id : Identifier<Id>> {
    /**
     * Analyzes the sort order of the given dataset.
     * @param ids The list of identifiers to determine the sort order for.
     * @return The calculated [Order].
     */
    operator fun invoke(ids: List<Id?>): Order

    /**
     * Represents the possible sort orders of a dataset.
     */
    enum class Order {
        ASCENDING,
        DESCENDING,
        UNSORTED,
        UNKNOWN
    }
}