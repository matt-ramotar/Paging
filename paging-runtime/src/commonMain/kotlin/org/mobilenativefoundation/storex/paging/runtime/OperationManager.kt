package org.mobilenativefoundation.storex.paging.runtime

interface OperationManager<Id : Identifier<Id>, K : Comparable<K>, V : Identifiable<Id>> {
    fun add(operation: Operation<Id, K, V>)
    fun remove(operation: Operation<Id, K, V>)
    fun removeAll(predicate: (Operation<Id, K, V>) -> Boolean)
    fun clear()
    fun get(): List<Operation<Id, K, V>>
    fun get(predicate: (Operation<Id, K, V>) -> Boolean)
}