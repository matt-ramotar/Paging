package org.mobilenativefoundation.storex.paging.internal.impl

import org.mobilenativefoundation.storex.paging.PagingState
import org.mobilenativefoundation.storex.paging.Quantifiable
import org.mobilenativefoundation.storex.paging.internal.api.StateHolder

class PagingStateHolder<Id : Comparable<Id>, Q: Quantifiable<Id>, E: Any>(
    initialState: PagingState<Id, Q, E>
) : StateHolder<PagingState<Id, Q, E>>(initialState)