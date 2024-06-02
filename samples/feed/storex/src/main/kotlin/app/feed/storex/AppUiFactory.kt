package app.feed.storex

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import app.feed.common.models.Post
import app.feed.common.models.PostId
import app.feed.common.ui.PostDetailScreen
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.Ui
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.CoroutineScope
import org.mobilenativefoundation.storex.paging.*

data object AppUiFactory : Ui.Factory {
    override fun create(screen: Screen, context: CircuitContext): Ui<*>? {
        return when (screen) {
            is HomeTab -> HomeTabUi
            is AccountTab -> AccountTabUi
            is PostDetailScreen -> PostDetailScreenUi
            else -> null
        }
    }
}


data object HomeTabUi : Ui<HomeTab.State> {
    @Composable
    override fun Content(state: HomeTab.State, modifier: Modifier) {

        Column {

            Row(modifier = Modifier.fillMaxWidth().padding(top = 24.dp), horizontalArrangement = Arrangement.Center) {
                Icon(
                    painterResource(app.feed.common.R.drawable.storex),
                    "storex",
                    modifier = Modifier.size(50.dp),
                    tint = Color(0xff212121)
                )
            }

            LazySelfUpdatingItems<String, PostId, Post, Throwable>(state.postIds) { itemState ->
                when (itemState.loadState) {
                    SingleLoadState.Cleared -> Text("SingleLoadState.Cleared")
                    is SingleLoadState.Error.InitialLoad -> Text("is SingleLoadState.Error.InitialLoad")
                    is SingleLoadState.Error.Refreshing -> Text("is SingleLoadState.Error.Refreshing")
                    SingleLoadState.Initial -> Text("SingleLoadState.Initial")
                    SingleLoadState.Loaded -> {

                        Column {
                            val item = itemState.item
                            if (item != null) {
                                app.feed.common.ui.PostListUi(item) {
                                    state.eventSink(HomeTab.Event.GoToDetailScreen(item.id))
                                }
                            } else {
                                Text("LOADED BUT NO ITEM!")
                            }

                            HorizontalDivider(
                                thickness = 1.dp,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
                                color = Color(0xfff3f3f3)
                            )
                        }
                    }

                    SingleLoadState.Loading -> Text("SingleLoadState.Loading")
                    SingleLoadState.Refreshing -> Text("SingleLoadState.Refreshing")
                }
            }
        }
    }

}

@Composable
fun PostUi(
    id: PostId?,
    modifier: Modifier = Modifier,
) {


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


@Composable
fun <Id : Comparable<Id>, Q : Quantifiable<Id>, V : Identifiable<Id, Q>, E : Any> LazySelfUpdatingItems(
    ids: ImmutableList<Q?>,
    modifier: Modifier = Modifier,
    content: @Composable (itemState: ItemState<Id, Q, V, E>) -> Unit
) {
    val items = remember(ids) {
        ids.map { id ->
            if (id == null) Nullable.Null
            else Nullable.NonNull(id)
        }
    }

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

sealed class Nullable<T : Any> {
    abstract val data: T?

    data class NonNull<T : Any>(
        override val data: T
    ) : Nullable<T>()

    data object Null : Nullable<Nothing>() {
        override val data = null
    }
}

data object AccountTabUi : Ui<AccountTab.State> {
    @Composable
    override fun Content(state: AccountTab.State, modifier: Modifier) {
        Text("StoreX - Account")
    }

}