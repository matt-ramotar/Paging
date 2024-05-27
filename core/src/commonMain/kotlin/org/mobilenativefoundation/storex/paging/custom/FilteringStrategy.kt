package org.mobilenativefoundation.storex.paging.custom

import org.mobilenativefoundation.store5.core.Identifiable

/**
 * A strategy for filtering the paged data based on specific filtering parameters.
 */
fun interface FilteringStrategy<Id : Comparable<Id>, V : Identifiable<Id>, P: Any> :
    TransformationStrategy<Id, V, P>