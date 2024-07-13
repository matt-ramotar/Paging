package org.mobilenativefoundation.storex.paging.runtime

import kotlinx.coroutines.flow.StateFlow

interface Pager<Id: Identifier<Id>> {
    val state: StateFlow<PagingState<Id>>
}
