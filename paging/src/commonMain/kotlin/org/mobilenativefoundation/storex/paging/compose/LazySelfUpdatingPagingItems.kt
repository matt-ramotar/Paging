package org.mobilenativefoundation.storex.paging.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CoroutineScope
import org.mobilenativefoundation.storex.paging.*


@OptIn(ExperimentalMaterialApi::class)
@Composable
fun <Id : Comparable<Id>, Q : Quantifiable<Id>, V : Identifiable<Id, Q>, E : Any> LazySelfUpdatingPagingItems(
    ids: List<Q?>,
    modifier: Modifier = Modifier,
    content: @Composable (itemState: ItemState<Id, Q, V, E>) -> Unit
) {
    val items = remember(ids) {
        ids.map { id ->
            if (id == null) Nullable.Null
            else Nullable.NonNull(id)
        }
    }

    Box(
        modifier = Modifier.pullRefresh(
            state = rememberPullRefreshState(
                onRefresh = {},
                refreshing = false
            )
        )
    ) {


        LazyColumn(modifier) {
            items(items, key = {
                when (it) {
                    is Nullable.NonNull -> it.data.value
                    Nullable.Null -> Unit
                }
            }) {
                val id = when (it) {
                    is Nullable.NonNull -> it.data
                    Nullable.Null -> null
                }

                SelfUpdatingItemContent<Id, Q, V, E>(id) { itemState ->
                    content(itemState)
                }
            }
        }
    }
}

sealed class Nullable<T : Any> {
    abstract val data: T?

    data class NonNull<T : Any>(
        override val data: T
    ) : Nullable<T>()

    data object Null : Nullable<Nothing>() {
        override val data = null
    }
}

@Composable
fun <Id : Comparable<Id>, Q : Quantifiable<Id>, V : Identifiable<Id, Q>, E : Any> SelfUpdatingItemContent(
    id: Q?,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    selfUpdatingItem: SelfUpdatingItem<Id, Q, V, E>? = rememberSelfUpdatingItem(id),
    content: @Composable (state: ItemState<Id, Q, V, E>) -> Unit
) {
    val itemState = selfUpdatingItem.stateIn(coroutineScope, key = selfUpdatingItem).collectAsState()
    content(itemState.value)
}
