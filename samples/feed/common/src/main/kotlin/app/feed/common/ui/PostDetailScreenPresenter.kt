package app.feed.common.ui

import androidx.compose.runtime.Composable
import app.feed.common.models.Post
import app.feed.common.models.PostId
import com.slack.circuit.runtime.presenter.Presenter

class PostDetailScreenPresenter(
    private val postId: PostId
) : Presenter<PostDetailScreen.State> {

    @Composable
    override fun present(): PostDetailScreen.State {
        return PostDetailScreen.State(
            postId
        )
    }
}