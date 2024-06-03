package app.feed.storex

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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


sealed interface HomeFeedSort {
    val id: String

    data object Best : HomeFeedSort {
        override val id: String = "best"
    }

    data object Hot : HomeFeedSort {
        override val id: String = "hot"
    }

    data class Top(
        val timespan: Timespan
    ) : HomeFeedSort {
        override val id: String = "top"
    }

    data object New : HomeFeedSort {
        override val id: String = "new"
    }
}

enum class Timespan {
    Hour,
    Day,
    Week,
    Month,
    Year,
    AllTime
}

@Composable
private fun HomeFeedSortDropdownMenu(
    sort: HomeFeedSort,
    onClick: (HomeFeedSort) -> Unit
) {

    var isExpanded by remember { mutableStateOf(false) }

    val iconResId = remember(sort) {
        when (sort) {
            HomeFeedSort.Best -> app.feed.common.R.drawable.trophy
            HomeFeedSort.Hot -> app.feed.common.R.drawable.hot
            HomeFeedSort.New -> app.feed.common.R.drawable.clock
            is HomeFeedSort.Top -> app.feed.common.R.drawable.top
        }
    }

    fun menuItemColors(sortId: String) = MenuItemColors(
        textColor = if (sortId == sort.id) Color(0xff229BF0) else Color.Black,
        leadingIconColor = if (sortId == sort.id) Color(0xff229BF0) else Color.Black,
        trailingIconColor = Color.Black,
        disabledTextColor = Color.Black,
        disabledLeadingIconColor = Color.Black,
        disabledTrailingIconColor = Color.Black
    )

    IconButton({
        isExpanded = !isExpanded
    }) {
        Icon(
            painterResource(iconResId),
            "sort icon",
            modifier = Modifier.size(32.dp),
            tint = Color(0xff229BF0)
        )


        DropdownMenu(expanded = isExpanded, onDismissRequest = {
            isExpanded = false
        }, modifier = Modifier.background(Color.White)) {
            DropdownMenuItem(text = {
                Text("Best")
            }, onClick = {
                onClick(HomeFeedSort.Best)
                isExpanded = false
            }, leadingIcon = {
                Icon(
                    painterResource(app.feed.common.R.drawable.trophy),
                    "trophy",
                    modifier = Modifier.size(24.dp)
                )
            }, colors = menuItemColors(HomeFeedSort.Best.id))

            DropdownMenuItem(text = {
                Text("Hot")
            }, onClick = {
                onClick(HomeFeedSort.Hot)
                isExpanded = false
            }, leadingIcon = {
                Icon(
                    painterResource(app.feed.common.R.drawable.hot),
                    "trophy",
                    modifier = Modifier.size(24.dp)
                )
            }, colors = menuItemColors(HomeFeedSort.Hot.id))
            DropdownMenuItem(text = {
                Text("Top")
            }, onClick = {
                onClick(HomeFeedSort.Top(Timespan.Day))
                isExpanded = false
            }, leadingIcon = {
                Icon(
                    painterResource(app.feed.common.R.drawable.top),
                    "trophy",
                    modifier = Modifier.size(24.dp)
                )
            }, colors = menuItemColors(HomeFeedSort.Top(Timespan.Day).id))

            DropdownMenuItem(text = {
                Text("New")
            }, onClick = {
                onClick(HomeFeedSort.New)
                isExpanded = false
            }, leadingIcon = {
                Icon(
                    painterResource(app.feed.common.R.drawable.clock),
                    "trophy",
                    modifier = Modifier.size(24.dp)
                )
            }, colors = menuItemColors(HomeFeedSort.New.id))


        }
    }

}


data object HomeTabUi : Ui<HomeTab.State> {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(state: HomeTab.State, modifier: Modifier) {

        Column(modifier = Modifier.fillMaxSize()) {

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp, start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Spacer(modifier = Modifier.width(32.dp))

                Icon(
                    painterResource(app.feed.common.R.drawable.storex),
                    "storex",
                    modifier = Modifier.size(40.dp),
                    tint = Color(0xff212121)
                )

                HomeFeedSortDropdownMenu(state.sort) {
                    state.eventSink(HomeTab.Event.UpdateSort(it))
                }

            }

            LazySelfUpdatingPagingItems<String, PostId, Post, Throwable>(state.postIds, {
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


data object AccountTabUi : Ui<AccountTab.State> {
    @Composable
    override fun Content(state: AccountTab.State, modifier: Modifier) {
        Text("StoreX - Account")
    }

}