package app.feed.storex

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.feed.common.models.Post
import app.feed.common.models.PostId
import app.feed.common.ui.PostDetailScreen
import app.feed.common.ui.PostDetailUi
import com.slack.circuit.runtime.ui.Ui
import kotlinx.coroutines.launch
import org.mobilenativefoundation.storex.paging.rememberSelfUpdatingItem
import org.mobilenativefoundation.storex.paging.stateIn

data object PostDetailScreenUi : Ui<PostDetailScreen.State> {
    @Composable
    override fun Content(state: PostDetailScreen.State, modifier: Modifier) {
        Column(modifier.fillMaxSize()) {

            val selfUpdatingItem = rememberSelfUpdatingItem<String, PostId, Post, Throwable>(state.postId)

            val coroutineScope = rememberCoroutineScope()

            val itemState = selfUpdatingItem.stateIn(coroutineScope, key = selfUpdatingItem)

            val item = itemState.value.item

            if (item == null) {
                Text("Loading")
            } else {
                PostDetailUi(item, modifier = Modifier.fillMaxSize().padding(bottom = 80.dp), { event ->
                    coroutineScope.launch {
                        selfUpdatingItem?.emit(event)
                    }
                }) { event ->

                    state.eventSink(event)

                }
            }
        }
    }

}





