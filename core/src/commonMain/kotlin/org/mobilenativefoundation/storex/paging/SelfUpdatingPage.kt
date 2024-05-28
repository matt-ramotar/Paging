package org.mobilenativefoundation.storex.paging

import androidx.compose.runtime.Composable
import org.mobilenativefoundation.store5.core.Identifiable

class SelfUpdatingPage<Id : Comparable<Id>, K: Any, V : Identifiable<Id>, E : Any>(

) {
    @Composable
    operator fun invoke(params: PagingSource.LoadParams<K>): PageState<Id, K, V, E> {
        TODO()
    }
}