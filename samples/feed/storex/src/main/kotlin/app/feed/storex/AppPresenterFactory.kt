package app.feed.storex

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import kotlinx.coroutines.flow.MutableStateFlow
import org.mobilenativefoundation.storex.paging.*

data class AppPresenterFactory(
    private val pager: Pager<String, PostId, GetFeedRequest, Post, Throwable>
) : Presenter.Factory {
    override fun create(screen: Screen, navigator: Navigator, context: CircuitContext): Presenter<*>? {
        return when (screen) {
            is HomeTab -> HomeTabPresenter(navigator, pager)
            is AccountTab -> AccountTabPresenter()
            is PostDetailScreen -> PostDetailScreenPresenter(screen.postId, navigator)
            else -> null
        }
    }

}


class HomeTabPresenter(
    private val navigator: Navigator,
    private val pager: Pager<String, PostId, GetFeedRequest, Post, Throwable>
) :
    Presenter<HomeTab.State> {

    private val requests = MutableSharedFlow<PagingRequest<GetFeedRequest>>(replay = 20)

    private var sort = MutableStateFlow<HomeFeedSort>(HomeFeedSort.New)

    @Composable
    override fun present(): HomeTab.State {

        val pagingState by pager.collectAsState()
        val sortState by sort.collectAsState()

        return HomeTab.State("", pagingState.ids.toImmutableList(), sort = sortState) { event ->

            when (event) {
                HomeTab.Event.Refresh -> {
                    println("*() Received event $event")

                    pagingState.eventSink(
                        PagingRequest.skipQueue(
                            key = GetFeedRequest(
                                cursor = null,
                                size = 20,
                                headers = mutableMapOf("type" to "refresh")
                            ),
                            direction = LoadDirection.Prepend,
                            strategy = LoadStrategy.SkipCache,
                        )
                    )
                }

                is HomeTab.Event.GoToDetailScreen -> navigator.goTo(
                    PostDetailScreen(event.postId)
                )

                is HomeTab.Event.UpdateSort -> {
                    sort.value = event.sort
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

