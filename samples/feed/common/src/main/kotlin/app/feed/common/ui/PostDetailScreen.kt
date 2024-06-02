package app.feed.common.ui

import android.os.Parcelable
import app.feed.common.models.Post
import app.feed.common.models.PostId
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.Screen
import kotlinx.parcelize.Parcelize


@Parcelize
data class PostDetailScreen(
    val postId: PostId
) : Screen, Parcelable {
    data class State(
        val postId: PostId
    ) : CircuitUiState

    sealed interface Event : CircuitUiEvent {
        data object Refresh : Event
    }
}