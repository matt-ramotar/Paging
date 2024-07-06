package org.mobilenativefoundation.storex.paging.runtime.internal.store.api

import kotlinx.coroutines.flow.Flow
import org.mobilenativefoundation.storex.paging.runtime.Identifiable
import org.mobilenativefoundation.storex.paging.runtime.Identifier
import org.mobilenativefoundation.storex.paging.runtime.ItemState
import org.mobilenativefoundation.storex.paging.runtime.UpdatingItem

/**
 * Presenter for managing [UpdatingItem] instances.
 */
internal interface UpdatingItemPresenter<Id : Identifier<Id>, V : Identifiable<Id>> {
    fun present(
        id: Id
    ): ItemState<Id, V>
}


