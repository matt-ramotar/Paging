package org.mobilenativefoundation.storex.paging.runtime.internal.updatingItem.api

import org.mobilenativefoundation.storex.paging.runtime.Identifiable
import org.mobilenativefoundation.storex.paging.runtime.Identifier
import org.mobilenativefoundation.storex.paging.runtime.UpdatingItem

internal interface UpdatingItemFactory<Id : Identifier<Id>, V : Identifiable<Id>> {
    fun create(id: Id): UpdatingItem<Id, V>
}