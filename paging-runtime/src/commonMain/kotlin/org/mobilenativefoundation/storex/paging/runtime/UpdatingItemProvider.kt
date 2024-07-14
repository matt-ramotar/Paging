package org.mobilenativefoundation.storex.paging.runtime


interface UpdatingItemProvider<Id : Identifier<Id>, V : Identifiable<Id>> {
    suspend fun get(id: Id): UpdatingItem<Id, V>
    suspend fun remove(id: Id)
    suspend fun clear()
}
