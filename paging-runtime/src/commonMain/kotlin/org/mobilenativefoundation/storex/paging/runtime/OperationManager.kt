package org.mobilenativefoundation.storex.paging.runtime

interface OperationManager<Id : Identifier<Id>, K : Comparable<K>, V : Identifiable<Id>> {
    suspend fun add(operation: Operation<Id, K, V>)
    suspend fun remove(operation: Operation<Id, K, V>)
    suspend fun removeAll(predicate: (Operation<Id, K, V>) -> Boolean)
    suspend fun clear()
    fun get(): List<Operation<Id, K, V>>
    fun get(predicate: (Operation<Id, K, V>) -> Boolean): List<Operation<Id, K, V>>
}