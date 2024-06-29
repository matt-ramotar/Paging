package app.feed.androidx

import androidx.compose.runtime.Composable
import androidx.paging.Pager
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.filter
import app.feed.common.HomeFeedSort
import app.feed.common.HomeTab
import app.feed.common.models.GetFeedRequest
import app.feed.common.models.Post
import app.feed.common.ui.PostDetailScreen
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

data class AppPresenterFactory(
    private val pager: Pager<GetFeedRequest, Post>
) : Presenter.Factory {
    override fun create(
        screen: Screen,
        navigator: Navigator,
        context: CircuitContext
    ): Presenter<*>? {
        return when (screen) {
            is HomeTab -> HomeTabPresenter(navigator, pager)
            is AccountTab -> AccountTabPresenter()
            else -> null
        }
    }

}

private fun isWithinRange(timestamp: Long?, range: Duration): Boolean {
    if (timestamp == null) return false

    val now = System.currentTimeMillis()
    val durationMillis = range.inWholeMilliseconds
    return now - timestamp <= durationMillis
}

// TODO(): Is it possible to sort?
@Composable
fun getBest(pagingDataFlow: Flow<PagingData<Post>>): LazyPagingItems<Post> {
    return pagingDataFlow.map { pagingData ->
        pagingData.filter { isWithinRange(it.createdAt, 1.hours) }
    }.collectAsLazyPagingItems()
}


class HomeTabPresenter(
    private val navigator: Navigator,
    private val pager: Pager<GetFeedRequest, Post>
) : Presenter<AndroidXHomeTabState> {

    @Composable
    override fun present(): AndroidXHomeTabState {

        val pagingData = getBest(pagingDataFlow = pager.flow)

        return HomeTab.State(
            "",
            pagingData,
            HomeFeedSort.New,
        ) { event ->
            when (event) {
                is HomeTab.Event.GoToDetailScreen -> {
                    navigator.goTo(
                        PostDetailScreen(event.postId)
                    )
                }

                HomeTab.Event.Refresh -> {
                    // TODO()
                }

                is HomeTab.Event.UpdateSearchQuery -> {
                    // TODO()
                }

                is HomeTab.Event.UpdateSort -> {
                    // TODO()
                }
            }
        }
    }
}

class AccountTabPresenter() : Presenter<AccountTab.State> {

    @Composable
    override fun present(): AccountTab.State {
        return AccountTab.State("")
    }
}

