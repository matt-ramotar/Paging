package org.mobilenativefoundation.storex.paging.internal.impl

import org.mobilenativefoundation.storex.paging.Identifier
import org.mobilenativefoundation.storex.paging.PagingState
import org.mobilenativefoundation.storex.paging.internal.api.StateHolder

class PagingStateHolder<Id : Identifier<*>>(
    initialState: PagingState<Id>
) : StateHolder<PagingState<Id>>(initialState)