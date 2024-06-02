package app.feed.storex

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import app.feed.common.models.GetFeedRequest
import app.feed.common.models.Post
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import org.mobilenativefoundation.storex.paging.*

data class AppPresenterFactory(
    private val pager: Pager<String, GetFeedRequest, Post, Throwable>
) : Presenter.Factory {
    override fun create(screen: Screen, navigator: Navigator, context: CircuitContext): Presenter<*>? {
        return when (screen) {
            is HomeTab -> HomeTabPresenter(pager)
            is AccountTab -> AccountTabPresenter()
            else -> null
        }
    }

}


class HomeTabPresenter(private val pager: Pager<String, GetFeedRequest, Post, Throwable>) : Presenter<HomeTab.State> {

    private val requests = MutableSharedFlow<PagingRequest<GetFeedRequest>>(replay = 1)

    @Composable
    override fun present(): HomeTab.State {

        val scope = rememberCoroutineScope()

        val pagingState = pager.pagingState(requests).collectAsState()

        return HomeTab.State("", pagingState.value.ids) { event ->

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

