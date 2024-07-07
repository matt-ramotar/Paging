package org.mobilenativefoundation.storex.paging.runtime.internal.updatingItem.impl

import org.mobilenativefoundation.storex.paging.runtime.Identifiable
import org.mobilenativefoundation.storex.paging.runtime.Identifier
import org.mobilenativefoundation.storex.paging.runtime.UpdatingItem
import org.mobilenativefoundation.storex.paging.runtime.internal.store.api.UpdatingItemPresenter
import org.mobilenativefoundation.storex.paging.runtime.internal.updatingItem.api.UpdatingItemFactory

internal class RealUpdatingItemFactory<Id : Identifier<Id>, V : Identifiable<Id>>(
    private val presenter: UpdatingItemPresenter<Id, V>
): UpdatingItemFactory<Id, V> {
    override fun create(id: Id): UpdatingItem<Id, V> {
        return RealUpdatingItem(id, presenter)
    }

}