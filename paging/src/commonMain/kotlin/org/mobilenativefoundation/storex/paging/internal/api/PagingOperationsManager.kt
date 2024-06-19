package org.mobilenativefoundation.storex.paging.internal.api

import org.mobilenativefoundation.storex.paging.Identifiable
import org.mobilenativefoundation.storex.paging.Identifier
import org.mobilenativefoundation.storex.paging.custom.Operation

// TODO(matt.ramotar@uber.com): Support local operations and network operations
interface PagingOperationsManager<Id : Identifier<*>, K : Any, V : Identifiable<Id>> {
    fun addOperation(operation: Operation<Id, K, V>)
    fun removeOperation(operation: Operation<Id, K, V>)
    fun removeAll(predicate: (Operation<Id, K, V>) -> Boolean)
    fun clearOperations()
}


