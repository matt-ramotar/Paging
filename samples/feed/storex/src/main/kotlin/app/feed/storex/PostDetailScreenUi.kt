package app.feed.storex

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.feed.common.models.Post
import app.feed.common.models.PostId
import app.feed.common.ui.PostDetailScreen
import app.feed.common.ui.PostDetailUi
import com.slack.circuit.runtime.ui.Ui

data object PostDetailScreenUi : Ui<PostDetailScreen.State> {
    @Composable
    override fun Content(state: PostDetailScreen.State, modifier: Modifier) {
        Column {

            SelfUpdatingItemContent<String, PostId, Post, Throwable>(state.postId) {
                val item = it.item
                if (item == null) {
                    Text("Loading")
                } else {
                    PostDetailUi(item)
                }
            }
        }
    }

}