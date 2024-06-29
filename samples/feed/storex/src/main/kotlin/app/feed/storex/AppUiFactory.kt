package app.feed.storex

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import app.feed.common.AccountTab
import app.feed.common.CommonHomeTab
import app.feed.common.HomeTab
import app.feed.common.models.Post
import app.feed.common.models.PostId
import app.feed.common.ui.PostDetailScreen
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.Ui
import org.mobilenativefoundation.storex.paging.SingleLoadState
import org.mobilenativefoundation.storex.paging.compose.LazySelfUpdatingPagingItems

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


data object HomeTabUi : Ui<StoreXHomeTabState> {
    @Composable
    override fun Content(state: StoreXHomeTabState, modifier: Modifier) {

        CommonHomeTab(state = state,
        appIcon = {
            Icon(
                painterResource(app.feed.common.R.drawable.storex),
                "storex",
                modifier = Modifier.size(40.dp),
                tint = Color(0xff212121)
            )
        },
            modifier = modifier) {
            LazySelfUpdatingPagingItems<String, PostId, Post, Throwable>(state.pagingData, {
                state.eventSink(HomeTab.Event.Refresh)
            }) { itemState ->
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
                                modifier = Modifier
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .fillMaxWidth(),
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


data object AccountTabUi : Ui<AccountTab.State> {
    @Composable
    override fun Content(state: AccountTab.State, modifier: Modifier) {
        Text("StoreX - Account")
    }

}