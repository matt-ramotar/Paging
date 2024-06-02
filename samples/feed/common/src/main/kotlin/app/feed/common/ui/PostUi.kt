package app.feed.common.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.feed.common.R
import app.feed.common.models.Post
import kotlinx.datetime.*
import kotlin.math.absoluteValue


fun timeAgo(dateTime: LocalDateTime): String {
    val then = dateTime.toInstant(TimeZone.currentSystemDefault())
    val now = Clock.System.now()
    val duration = then.periodUntil(now, TimeZone.currentSystemDefault())

    return when {
        duration.seconds < 60 -> "${duration.seconds} seconds ago"
        duration.minutes < 60 -> "${duration.minutes} minutes ago"
        duration.hours < 24 -> "${duration.hours} hours ago"
        duration.days < 7 -> "${duration.days} days ago"
        duration.days < 30 -> "${duration.days / 7} weeks ago"
        duration.days < 365 -> "${duration.days / 30} months ago"
        else -> "${duration.days / 365} years ago"
    }
}

@Composable
fun PostListUi(post: Post) {

    Row(modifier = Modifier.padding(16.dp)) {

        Image(
            painterResource(R.drawable.tag), "Tag", modifier = Modifier.size(60.dp).clip(
                CircleShape
            ),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column {

            Row {
                Text("Tag Ramotar", fontWeight = FontWeight.Bold)

                Spacer(modifier = Modifier.width(8.dp))

                Text("@tag", color = Color(0xff424242))

                Spacer(modifier = Modifier.width(8.dp))

                Text("${post.createdAt.monthNumber}/${post.createdAt.dayOfMonth}", color = Color(0xff424242))
            }

            Spacer(modifier = Modifier.width(16.dp))


            Text(post.text, color = Color.Black)

            Spacer(modifier = Modifier.width(16.dp))

            Text("#p${post.id.value} #g${post.id.value.toInt().mod(10)}", color = Color(0xff229BF0))

            Spacer(modifier = Modifier.width(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Row(verticalAlignment = Alignment.CenterVertically) {
                    CommentIcon()
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(post.commentCount.toString(), color = Color(0xff424242))
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RetweetIcon()
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(post.retweetCount.toString(), color = Color(0xff424242))
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    HeartIcon()
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(post.favoriteCount.toString(), color = Color(0xff424242))
                }
            }

        }
    }
}

@Composable
fun CommentIcon() {
    val icon = painterResource(R.drawable.comment)
    Icon(icon, "comment", modifier = Modifier.size(24.dp), tint = Color(0xff424242))
}

@Composable
fun HeartIcon() {
    val icon = painterResource(R.drawable.heart)
    Icon(icon, "heart", modifier = Modifier.size(24.dp), tint = Color(0xff424242))
}

@Composable
fun RetweetIcon() {
    val icon = painterResource(R.drawable.repost)
    Icon(icon, "retweet", modifier = Modifier.size(24.dp), tint = Color(0xff424242))
}

@Composable
fun PostDetailUi(post: Post) {
    Column {
        Text("Tag Ramotar")
        Text("@tag")

        Text(post.text)


        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(post.commentCount.toString(), fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Comments")
            }

            Row(verticalAlignment = Alignment.CenterVertically) {

                Text(post.retweetCount.toString(), fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reposts")
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(post.favoriteCount.toString(), fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Likes")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                CommentIcon()
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                RetweetIcon()
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                HeartIcon()
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                HeartIcon()
            }
        }
    }
}