package app.feed.androidx

import androidx.compose.runtime.Composable
import androidx.paging.Pager
import androidx.paging.compose.collectAsLazyPagingItems
import app.feed.common.HomeFeedSort
import app.feed.common.HomeTab
import app.feed.common.models.GetFeedRequest
import app.feed.common.models.Post
import app.feed.common.ui.PostDetailScreen
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen

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


class HomeTabPresenter(
    private val navigator: Navigator,
    private val pager: Pager<GetFeedRequest, Post>
) : Presenter<AndroidXHomeTabState> {

    @Composable
    override fun present(): AndroidXHomeTabState {

        val pagingData = pager.flow.collectAsLazyPagingItems()

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

