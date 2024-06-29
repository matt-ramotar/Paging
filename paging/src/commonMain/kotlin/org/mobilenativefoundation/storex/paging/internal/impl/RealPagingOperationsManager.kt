package org.mobilenativefoundation.storex.paging.internal.impl

import org.mobilenativefoundation.storex.paging.Identifiable
import org.mobilenativefoundation.storex.paging.Identifier
import org.mobilenativefoundation.storex.paging.custom.Operation
import org.mobilenativefoundation.storex.paging.internal.api.PagingOperationsManager

class RealPagingOperationsManager<Id : Identifier<*>, K : Any, V : Identifiable<Id>> :
    PagingOperationsManager<Id, K, V> {

    private val operations = mutableListOf<Operation<Id, K, V>>()


    override fun addOperation(operation: Operation<Id, K, V>) {
        operations.add(operation)
    }

    override fun removeOperation(operation: Operation<Id, K, V>) {
        operations.remove(operation)
    }

    override fun removeAll(predicate: (Operation<Id, K, V>) -> Boolean) {
        operations.removeAll(predicate)
    }

    override fun clearOperations() {
        operations.clear()
    }
}