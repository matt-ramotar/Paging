package app.feed.storex

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import app.feed.common.models.GetFeedRequest
import app.feed.common.models.Post
import app.feed.common.models.PostId
import app.feed.common.ui.PostDetailScreen
import app.feed.common.ui.PostDetailScreenPresenter
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import org.mobilenativefoundation.storex.paging.LoadDirection
import org.mobilenativefoundation.storex.paging.LoadStrategy
import org.mobilenativefoundation.storex.paging.Pager
import org.mobilenativefoundation.storex.paging.PagingRequest

data class AppPresenterFactory(
    private val pager: Pager<String, PostId, GetFeedRequest, Post, Throwable>
) : Presenter.Factory {
    override fun create(screen: Screen, navigator: Navigator, context: CircuitContext): Presenter<*>? {
        return when (screen) {
            is HomeTab -> HomeTabPresenter(navigator, pager)
            is AccountTab -> AccountTabPresenter()
            is PostDetailScreen -> PostDetailScreenPresenter(screen.postId)
            else -> null
        }
    }

}


class HomeTabPresenter(
    private val navigator: Navigator,
    private val pager: Pager<String, PostId, GetFeedRequest, Post, Throwable>
) :
    Presenter<HomeTab.State> {

    private val requests = MutableSharedFlow<PagingRequest<GetFeedRequest>>(replay = 1)

    @Composable
    override fun present(): HomeTab.State {

        val scope = rememberCoroutineScope()

        val pagingState = pager.pagingStateFlow(scope, requests).collectAsState()

        return HomeTab.State("", pagingState.value.ids.toImmutableList()) { event ->

            when (event) {
                HomeTab.Event.Refresh -> {
                    scope.launch {
                        requests.emit(
                            PagingRequest.skipQueue(
                                key = GetFeedRequest(cursor = null, size = 20),
                                direction = LoadDirection.Prepend,
                                strategy = LoadStrategy.SkipCache
                            )
                        )
                    }
                }

                is HomeTab.Event.GoToDetailScreen -> navigator.goTo(
                    PostDetailScreen(event.postId)
                )
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

