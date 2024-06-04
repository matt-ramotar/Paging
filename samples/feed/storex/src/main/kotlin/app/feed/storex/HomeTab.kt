package app.feed.storex

import android.os.Parcelable
import app.feed.common.models.Post
import app.feed.common.models.PostId
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.Screen
import kotlinx.collections.immutable.ImmutableList
import kotlinx.parcelize.Parcelize


@Parcelize
data object HomeTab : Screen, Parcelable {
    data class State(
        val userId: String,
        val postIds: ImmutableList<PostId?>,
        val sort: HomeFeedSort,
        val eventSink: (event: Event) -> Unit
    ) : CircuitUiState

    sealed interface Event : CircuitUiEvent {
        data object Refresh : Event
        data class GoToDetailScreen(val postId: PostId): Event
        data class UpdateSort(val sort: HomeFeedSort): Event
        data class UpdateSearchQuery(val searchQuery: String?): Event
    }
}