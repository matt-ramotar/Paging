package org.mobilenativefoundation.storex.paging.internal.api

import org.mobilenativefoundation.storex.paging.Identifiable
import org.mobilenativefoundation.storex.paging.Quantifiable
import org.mobilenativefoundation.storex.paging.custom.Operation

interface OperationManager<Id : Comparable<Id>, Q : Quantifiable<Id>, K : Any, V : Identifiable<Id, Q>> {
    fun addOperation(operation: Operation<Id, Q, K, V>)
    fun removeOperation(operation: Operation<Id, Q, K, V>)
    fun getOperations(): List<Operation<Id, Q, K, V>>
}


class RealOperationManager<Id : Comparable<Id>, Q : Quantifiable<Id>, K : Any, V : Identifiable<Id, Q>> :
    OperationManager<Id, Q, K, V> {

    private val operations = mutableListOf<Operation<Id, Q, K, V>>()

    override fun addOperation(operation: Operation<Id, Q, K, V>) {
        operations.add(operation)
    }

    override fun removeOperation(operation: Operation<Id, Q, K, V>) {
        operations.remove(operation)
    }

    override fun getOperations(): List<Operation<Id, Q, K, V>> = operations
}