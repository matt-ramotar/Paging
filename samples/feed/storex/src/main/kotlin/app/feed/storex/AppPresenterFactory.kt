package app.feed.storex

import androidx.compose.runtime.*
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
import org.mobilenativefoundation.storex.paging.*
import org.mobilenativefoundation.storex.paging.custom.Operation
import kotlin.time.Duration
import kotlin.time.Duration.Companion.INFINITE
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

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

data object SortedByRelevanceScore : Operation<String, PostId, GetFeedRequest, Post>() {
    override operator fun invoke(snapshot: ItemSnapshotList<String, PostId, Post>): ItemSnapshotList<String, PostId, Post> {
        return ItemSnapshotList(snapshot.sortedBy { it?.relevanceScore })
    }
}

data object SortedByTrendingScore : Operation<String, PostId, GetFeedRequest, Post>() {
    override operator fun invoke(snapshot: ItemSnapshotList<String, PostId, Post>): ItemSnapshotList<String, PostId, Post> {
        return ItemSnapshotList(snapshot.sortedBy { it?.trendingScore })
    }
}

data object SortedByDateTimeCreated : Operation<String, PostId, GetFeedRequest, Post>() {
    override operator fun invoke(snapshot: ItemSnapshotList<String, PostId, Post>): ItemSnapshotList<String, PostId, Post> {
        return ItemSnapshotList(snapshot.sortedBy { it?.createdAt })
    }
}

enum class TimeRange(val duration: Duration) {
    HOUR(1.hours),
    DAY(1.days),
    WEEK(7.days),
    MONTH(31.days),
    YEAR(365.days),
    INF(INFINITE)
}


class TopPosts(private val timeRange: TimeRange) : Operation<String, PostId, GetFeedRequest, Post>() {

    private fun isWithinRange(timestamp: Long?, range: TimeRange): Boolean {
        if (timestamp == null) return false

        val now = System.currentTimeMillis()
        val durationMillis = range.duration.inWholeMilliseconds
        return now - timestamp <= durationMillis
    }

    override operator fun invoke(snapshot: ItemSnapshotList<String, PostId, Post>): ItemSnapshotList<String, PostId, Post> {
        val filteredAndSorted = snapshot.filter { isWithinRange(it?.createdAt, timeRange) }
            .sortedByDescending { it?.favoriteCount }
        return ItemSnapshotList(filteredAndSorted)
    }
}


class HomeTabPresenter(
    private val navigator: Navigator,
    private val pager: Pager<String, PostId, GetFeedRequest, Post, Throwable>
) :
    Presenter<HomeTab.State> {

    private val requests = MutableSharedFlow<PagingRequest<GetFeedRequest>>(replay = 20)

    @Composable
    override fun present(): HomeTab.State {

        val pagingState by pager.collectAsState()
        var sort by remember { mutableStateOf<HomeFeedSort>(HomeFeedSort.New) }


        LaunchedEffect(sort) {
            val sortingOperation = when (sort) {
                HomeFeedSort.Best -> SortedByRelevanceScore
                HomeFeedSort.Hot -> SortedByTrendingScore
                HomeFeedSort.New -> SortedByDateTimeCreated
                is HomeFeedSort.Top -> {
                    val s = sort as HomeFeedSort.Top
                    val timespan = s.timespan
                    when (timespan) {
                        Timespan.Hour -> TopPosts(TimeRange.HOUR)
                        Timespan.Day -> TopPosts(TimeRange.DAY)
                        Timespan.Week -> TopPosts(TimeRange.WEEK)
                        Timespan.Month -> TopPosts(TimeRange.MONTH)
                        Timespan.Year -> TopPosts(TimeRange.YEAR)
                        Timespan.AllTime -> TopPosts(TimeRange.INF)
                    }
                }
            }

            pager.clearOperations()
            pager.addOperation(sortingOperation)
        }

        return HomeTab.State("", pagingState.ids.toImmutableList(), sort = sort) { event ->

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
                    sort = event.sort
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

