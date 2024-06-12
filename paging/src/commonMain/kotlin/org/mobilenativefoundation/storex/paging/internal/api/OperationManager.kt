package org.mobilenativefoundation.storex.paging.internal.api

import org.mobilenativefoundation.storex.paging.Identifiable
import org.mobilenativefoundation.storex.paging.Quantifiable
import org.mobilenativefoundation.storex.paging.custom.Operation

// TODO(matt.ramotar@uber.com): Support local operations and network operations
interface OperationManager<Id : Comparable<Id>, Q : Quantifiable<Id>, K : Any, V : Identifiable<Id, Q>> {
    fun addOperation(operation: Operation<Id, Q, K, V>)
    fun removeOperation(operation: Operation<Id, Q, K, V>)
    fun removeAll(predicate: (Operation<Id, Q, K, V>) -> Boolean)
    fun clearOperations()
    fun applyOperationsAndUpdateState()

}


class RealOperationManager<Id : Comparable<Id>, Q : Quantifiable<Id>, K : Any, V : Identifiable<Id, Q>>: OperationManager<Id, Q, K, V> {
    override fun addOperation(operation: Operation<Id, Q, K, V>) {
        TODO("Not yet implemented")
    }

    override fun removeOperation(operation: Operation<Id, Q, K, V>) {
        TODO("Not yet implemented")
    }

    override fun removeAll(predicate: (Operation<Id, Q, K, V>) -> Boolean) {
        TODO("Not yet implemented")
    }

    override fun clearOperations() {
        TODO("Not yet implemented")
    }

    override fun applyOperationsAndUpdateState() {
        TODO("Not yet implemented")
    }

}