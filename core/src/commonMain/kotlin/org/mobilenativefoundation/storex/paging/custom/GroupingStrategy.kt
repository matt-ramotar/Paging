package org.mobilenativefoundation.storex.paging.custom

import org.mobilenativefoundation.store5.core.Identifiable

/**
 * A strategy for grouping the paged data based on a specific criteria or field.
 */
fun interface GroupingStrategy<Id : Comparable<Id>, V : Identifiable<Id>, P: Any> :
    TransformationStrategy<Id, V, P>