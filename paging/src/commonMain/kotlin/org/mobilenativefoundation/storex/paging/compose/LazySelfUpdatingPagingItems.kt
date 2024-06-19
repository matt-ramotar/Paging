package org.mobilenativefoundation.storex.paging.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.mobilenativefoundation.storex.paging.*


@OptIn(ExperimentalMaterialApi::class)
@Composable
fun <Id : Identifier<Id>, V : Identifiable<Id>> LazySelfUpdatingPagingItems(
    ids: List<Id?>,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (itemState: ItemState<Id, V>) -> Unit
) {
    val items = remember(ids) {
        ids.map { id ->
            if (id == null) Nullable.Null
            else Nullable.NonNull(id)
        }
    }

    val refreshScope = rememberCoroutineScope()
    var refreshing by remember { mutableStateOf(false) }


    fun refresh() = refreshScope.launch {
        refreshing = true
        println("*** ABOUT TO INVOKE ON REFRESH")
        delay(1000)
        onRefresh()
        refreshing = false
    }

    val state = rememberPullRefreshState(refreshing, ::refresh)

    Box(
        modifier = Modifier.fillMaxSize().pullRefresh(state)
    ) {
        LazyColumn(modifier) {
            items(items) {
                val id = when (it) {
                    is Nullable.NonNull -> it.data
                    Nullable.Null -> null
                }

                SelfUpdatingItemContent<Id, V>(id) { itemState ->
                    content(itemState)
                }
            }
        }

        PullRefreshIndicator(refreshing, state, Modifier.align(Alignment.TopCenter))
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
fun <Id : Identifier<Id>, V : Identifiable<Id>> SelfUpdatingItemContent(
    id: Id?,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    selfUpdatingItem: SelfUpdatingItem<Id, V>? = rememberSelfUpdatingItem(id),
    content: @Composable (state: ItemState<Id, V>) -> Unit
) {
    val itemState = selfUpdatingItem.stateIn(coroutineScope, key = selfUpdatingItem).collectAsState()
    content(itemState.value)
}
