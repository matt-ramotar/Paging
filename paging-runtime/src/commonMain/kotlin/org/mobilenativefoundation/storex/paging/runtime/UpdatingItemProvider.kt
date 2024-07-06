package org.mobilenativefoundation.storex.paging.runtime


interface UpdatingItemProvider<Id : Identifier<Id>, V : Identifiable<Id>> {
    operator fun get(id: Id): UpdatingItem<Id, V>
}
