package org.mobilenativefoundation.storex.paging.custom

import org.mobilenativefoundation.store5.core.Identifiable

/**
 * A strategy for validating the paged data against specific rules or constraints.
 */
fun interface ValidationStrategy<Id : Comparable<Id>, V : Identifiable<Id>, P : Any> :
    TransformationStrategy<Id, V, P>