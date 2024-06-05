package app.feed.androidx

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import app.feed.common.CommonHomeTab
import app.feed.common.HomeTab
import app.feed.common.models.Post
import com.slack.circuit.runtime.ui.Ui


typealias AndroidXHomeTabState = HomeTab.State<LazyPagingItems<Post>>

data object HomeTabUi : Ui<AndroidXHomeTabState> {
    @Composable
    override fun Content(state: AndroidXHomeTabState, modifier: Modifier) {

        CommonHomeTab(
            state = state,
            appIcon = {
                Icon(
                    painterResource(R.drawable.androidx),
                    "androidx",
                    modifier = Modifier.size(40.dp),
                    tint = Color.Unspecified
                )
            },

            modifier = modifier
        ) {

            LazyColumn {
                items(count = state.pagingData.itemCount) { index ->
                    val item = state.pagingData[index]
                    if (item == null) {

                    } else {
                        Column {

                            app.feed.common.ui.PostListUi(item) {
                                state.eventSink(HomeTab.Event.GoToDetailScreen(item.id))
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
                }
            }
        }
    }

}
