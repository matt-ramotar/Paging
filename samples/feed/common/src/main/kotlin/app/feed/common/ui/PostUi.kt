package app.feed.common.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import app.feed.common.models.Post

@Composable
fun PostUi(post: Post){
    Row {
        Text(post.text)
    }
}