package org.mobilenativefoundation.storex.paging.custom

import org.mobilenativefoundation.store5.core.Identifiable

/**
 * A strategy for sorting the paged data based on specific sorting parameters.
 */
fun interface SortingStrategy<Id : Comparable<Id>, V : Identifiable<Id>, P: Any> :
    TransformationStrategy<Id, V, P>