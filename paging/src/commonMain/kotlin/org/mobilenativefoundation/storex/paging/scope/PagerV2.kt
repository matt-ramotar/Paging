package org.mobilenativefoundation.storex.paging.scope

import kotlinx.coroutines.flow.Flow
import org.mobilenativefoundation.storex.paging.Identifier
import org.mobilenativefoundation.storex.paging.PagingState


interface PagerV2<Id : Identifier<Id>> {

    val flow: Flow<PagingState<Id>>
}

