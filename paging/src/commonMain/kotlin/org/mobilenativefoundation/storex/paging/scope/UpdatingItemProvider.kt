package org.mobilenativefoundation.storex.paging.scope

import org.mobilenativefoundation.storex.paging.Identifiable
import org.mobilenativefoundation.storex.paging.Identifier

interface UpdatingItemProvider<Id : Identifier<Id>, V : Identifiable<Id>> {
    operator fun get(id: Id): UpdatingItemV2<Id, V>
}

class RealUpdatingItemProvider<Id : Identifier<Id>, V : Identifiable<Id>>: UpdatingItemProvider<Id, V> {
    override fun get(id: Id): UpdatingItemV2<Id, V> {
        TODO("Not yet implemented")
    }

}