package app.feed.storex

import android.os.Parcelable
import app.feed.common.models.PostId
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.Screen
import kotlinx.parcelize.Parcelize


@Parcelize
data object HomeTab : Screen, Parcelable {
    data class State(
        val userId: String,
        val postIds: List<PostId>,
    ) : CircuitUiState

    sealed interface Event : CircuitUiEvent {
        data object Refresh : Event
    }
}