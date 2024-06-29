package app.feed.common

import android.os.Parcelable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuItemColors
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import app.feed.common.models.PostId
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.Screen
import kotlinx.parcelize.Parcelize

@Parcelize
data object HomeTab : Screen, Parcelable {
    data class State<T : Any>(
        val userId: String,
        val pagingData: T,
        val sort: HomeFeedSort,
        val eventSink: (event: Event) -> Unit
    ) : CircuitUiState

    sealed interface Event : CircuitUiEvent {
        data object Refresh : Event
        data class GoToDetailScreen(val postId: PostId) : Event
        data class UpdateSort(val sort: HomeFeedSort) : Event
        data class UpdateSearchQuery(val searchQuery: String?) : Event
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

    data object Default : HomeFeedSort {
        override val id: String = "default"
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
            HomeFeedSort.Best -> R.drawable.trophy
            HomeFeedSort.Hot -> R.drawable.hot
            HomeFeedSort.New -> R.drawable.one_day
            is HomeFeedSort.Top -> R.drawable.top
            HomeFeedSort.Default -> R.drawable.clock
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


@Composable
fun <T : Any> CommonHomeTab(
    state: HomeTab.State<T>,
    appIcon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier.fillMaxSize()) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            Spacer(modifier = Modifier.width(32.dp))

            appIcon()

            HomeFeedSortDropdownMenu(state.sort) {
                state.eventSink(HomeTab.Event.UpdateSort(it))
            }

        }


        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
        ) {
            var value by remember { mutableStateOf("") }

            TextField(
                value = value,
                placeholder = {
                    Text("Search")
                },
                leadingIcon = {

                    Icon(
                        painterResource(app.feed.common.R.drawable.search),
                        "search",
                        modifier = Modifier.size(24.dp),
                    )
                },
                onValueChange = {
                    value = it
                    state.eventSink(HomeTab.Event.UpdateSearchQuery(it.ifEmpty { null }))
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xfff3f3f3),
                    unfocusedContainerColor = Color(0xfff3f3f3),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                textStyle = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(2.dp),
                singleLine = true,
                shape = RoundedCornerShape(8.dp)
            )
        }

        content()


    }
}