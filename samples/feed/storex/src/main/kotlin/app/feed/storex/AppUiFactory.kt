package app.feed.storex

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import app.feed.common.models.Post
import app.feed.common.models.PostId
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.Ui
import kotlinx.collections.immutable.ImmutableList
import org.mobilenativefoundation.storex.paging.SelfUpdatingItem
import org.mobilenativefoundation.storex.paging.SingleLoadState
import org.mobilenativefoundation.storex.paging.selfUpdatingItem
import org.mobilenativefoundation.storex.paging.stateIn

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

            PagingLazyColumn(state.postIds) {
                PostUi(it)
            }
        }
    }

}

@Composable
fun PostUi(id: PostId?, modifier: Modifier = Modifier, model: SelfUpdatingItem<String, PostId, Post, Throwable>? = selfUpdatingItem(id, key = id)) {
    val scope = rememberCoroutineScope()
    val state = model.stateIn(scope, key = model)

    when (state.value.loadState) {
        SingleLoadState.Cleared -> Text("SingleLoadState.Cleared")
        is SingleLoadState.Error.InitialLoad -> Text("is SingleLoadState.Error.InitialLoad")
        is SingleLoadState.Error.Refreshing -> Text("is SingleLoadState.Error.Refreshing")
        SingleLoadState.Initial -> Text("SingleLoadState.Initial")
        SingleLoadState.Loaded -> Text("${state.value.item?.title} - SingleLoadState.Loaded")
        SingleLoadState.Loading -> Text("SingleLoadState.Loading")
        SingleLoadState.Refreshing -> Text("SingleLoadState.Refreshing")
    }

}


@Composable
fun PagingLazyColumn(
    ids: ImmutableList<PostId?>,
    modifier: Modifier = Modifier,
    content: @Composable (PostId?) -> Unit
) {
    val items = remember(ids) {
        ids.map { id ->
            if (id == null) PagingId.Placeholder
            else PagingId.Data(id)
        }
    }

    LazyColumn {
        items(items) {
            val id = when (it) {
                is PagingId.Data -> it.data
                PagingId.Placeholder -> null
            }
            content(id)
        }
    }
}

sealed class PagingId {
    data class Data(
        val data: PostId
    ) : PagingId()

    data object Placeholder : PagingId()
}

data object AccountTabUi : Ui<AccountTab.State> {
    @Composable
    override fun Content(state: AccountTab.State, modifier: Modifier) {
        Text("StoreX - Account")
    }

}