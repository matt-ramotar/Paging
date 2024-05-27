package org.mobilenativefoundation.storex.paging.custom

import org.mobilenativefoundation.store5.core.Identifiable

/**
 * Represents a strategy for aggregating loaded pages of data.
 */
fun interface AggregatingStrategy<Id : Comparable<Id>, V : Identifiable<Id>, P: Any> :
    TransformationStrategy<Id, V, P>