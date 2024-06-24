package org.mobilenativefoundation.storex.paging.scope

import org.mobilenativefoundation.storex.paging.Identifiable
import org.mobilenativefoundation.storex.paging.Identifier
import org.mobilenativefoundation.storex.paging.custom.Operation

interface Operator<Id : Identifier<Id>, K : Any, V : Identifiable<Id>> {
    fun add(operation: Operation<Id, K, V>)
    fun remove(operation: Operation<Id, K, V>)
    fun removeAll(predicate: (Operation<Id, K, V>) -> Boolean)
    fun clear()
    fun get(): List<Operation<Id, K, V>>
    fun get(predicate: (Operation<Id, K, V>) -> Boolean)
}


class RealOperator<Id : Identifier<Id>, K : Comparable<K>, V : Identifiable<Id>>(
    private val operations: MutableList<Operation<Id, K, V>>
): Operator<Id, K, V> {
    override fun add(operation: Operation<Id, K, V>) {
        TODO("Not yet implemented")
    }

    override fun remove(operation: Operation<Id, K, V>) {
        TODO("Not yet implemented")
    }

    override fun removeAll(predicate: (Operation<Id, K, V>) -> Boolean) {
        TODO("Not yet implemented")
    }

    override fun clear() {
        TODO("Not yet implemented")
    }

    override fun get(): List<Operation<Id, K, V>> {
        TODO("Not yet implemented")
    }

    override fun get(predicate: (Operation<Id, K, V>) -> Boolean) {
        TODO("Not yet implemented")
    }

}