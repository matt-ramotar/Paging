package org.mobilenativefoundation.storex.paging.internal.api

import org.mobilenativefoundation.storex.paging.Identifiable
import org.mobilenativefoundation.storex.paging.Quantifiable
import org.mobilenativefoundation.storex.paging.custom.Operation

interface OperationManager<Id : Comparable<Id>, Q : Quantifiable<Id>, K : Any, V : Identifiable<Id, Q>> {
    fun addOperation(operation: Operation<Id, Q, K, V>)
    fun removeOperation(operation: Operation<Id, Q, K, V>)
    fun removeAll(predicate: (Operation<Id, Q, K, V>) -> Boolean)
    fun clearOperations()
}

