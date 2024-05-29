package org.mobilenativefoundation.storex.paging

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
fun <Id : Comparable<Id>, K : Any, V : Identifiable<Id>, E : Any> rememberSelfUpdatingItem(
    pager: Pager<Id, K, V, E>,
    id: Quantifiable<Id>
): SelfUpdatingItem<Id, V, E> {
    val updatingItem = remember(id) {
        pager.selfUpdatingItem(id)
    }

    return updatingItem
}