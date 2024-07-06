package org.mobilenativefoundation.storex.paging.runtime

import kotlinx.coroutines.flow.Flow

interface Pager<Id: Identifier<Id>> {
    val flow: Flow<PagingState<Id>>
}
