package org.mobilenativefoundation.storex.paging.custom

import org.mobilenativefoundation.store5.core.Identifiable

/**
 * A strategy for removing duplicate items from the paged data based on a specific criteria.
 */
fun interface DeduplicationStrategy<Id : Comparable<Id>, V : Identifiable<Id>, P: Any> :
    TransformationStrategy<Id, V, P>