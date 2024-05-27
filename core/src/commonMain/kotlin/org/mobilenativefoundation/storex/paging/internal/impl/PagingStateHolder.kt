package org.mobilenativefoundation.storex.paging.internal.impl

import org.mobilenativefoundation.storex.paging.PagingState
import org.mobilenativefoundation.storex.paging.internal.api.StateHolder

class PagingStateHolder<Id : Comparable<Id>, E: Any>(
    initialState: PagingState<Id, E>
) : StateHolder<PagingState<Id, E>>(initialState)