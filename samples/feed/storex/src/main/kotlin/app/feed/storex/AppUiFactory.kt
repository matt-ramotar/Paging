package app.feed.storex

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import app.feed.common.models.Post
import app.feed.common.models.PostId
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
            else -> null
        }
    }
}


data object HomeTabUi : Ui<HomeTab.State> {
    @Composable
    override fun Content(state: HomeTab.State, modifier: Modifier) {

        Column {
            Text("StoreX - Home")

            LazySelfUpdatingItems<String, PostId, Post, Throwable>(state.postIds) { itemState ->
                when (itemState.loadState) {
                    SingleLoadState.Cleared -> Text("SingleLoadState.Cleared")
                    is SingleLoadState.Error.InitialLoad -> Text("is SingleLoadState.Error.InitialLoad")
                    is SingleLoadState.Error.Refreshing -> Text("is SingleLoadState.Error.Refreshing")
                    SingleLoadState.Initial -> Text("SingleLoadState.Initial")
                    SingleLoadState.Loaded -> Text("${itemState.item?.title} - SingleLoadState.Loaded")
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
            if (id == null) NullableQ.Null
            else NullableQ.NonNull(id)
        }
    }

    LazyColumn(modifier) {
        items(items, key = {
            when (it) {
                is NullableQ.NonNull -> it.data.value
                NullableQ.Null -> Unit
            }
        }) {
            val id = when (it) {
                is NullableQ.NonNull -> it.data
                NullableQ.Null -> null
            }

            SelfUpdatingItemContent<Id, Q, V, E>(id) { itemState ->
                content(itemState)
            }
        }
    }
}

sealed class NullableQ<Id : Comparable<Id>, Q : Quantifiable<Id>> {
    abstract val data: Q?

    data class NonNull<Id : Comparable<Id>, Q : Quantifiable<Id>>(
        override val data: Q
    ) : NullableQ<Id, Q>()

    data object Null : NullableQ<Nothing, Nothing>() {
        override val data = null
    }
}

data object AccountTabUi : Ui<AccountTab.State> {
    @Composable
    override fun Content(state: AccountTab.State, modifier: Modifier) {
        Text("StoreX - Account")
    }

}