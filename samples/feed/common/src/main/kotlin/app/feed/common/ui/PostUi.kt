package app.feed.common.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
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
import app.feed.common.models.PostId
import kotlinx.datetime.*
import org.mobilenativefoundation.storex.paging.SelfUpdatingItem


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
fun PostListUi(post: Post, onSelect: () -> Unit) {

    val createdAt by remember {
        derivedStateOf {
            Instant.fromEpochMilliseconds(post.createdAt).toLocalDateTime(TimeZone.currentSystemDefault())
        }
    }

    Row(modifier = Modifier.clickable { onSelect() }.padding(16.dp)) {

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

                Text(
                    "${createdAt.month.number}/${createdAt.dayOfMonth}/${createdAt.year.mod(2000)}",
                    color = Color(0xff424242)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))


            Text(post.text, color = Color.Black)

            Spacer(modifier = Modifier.height(8.dp))

            Text("#p${post.id.value} #g${1 + (10 - post.id.value.toInt().mod(10)).mod(10)}", color = Color(0xff229BF0))

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Row(verticalAlignment = Alignment.CenterVertically) {
                    CommentIcon {}
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(post.commentCount.toString(), color = Color(0xff424242))
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RepostIcon {}
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(post.retweetCount.toString(), color = Color(0xff424242))
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    HeartIcon(
                        if (post.isLikedByViewer) LikedPostColor else UnlikedPostColor
                    ) {}
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        post.favoriteCount.toString(),
                        color = if (post.isLikedByViewer) LikedPostColor else UnlikedPostColor
                    )
                }
            }

        }
    }
}

@Composable
fun CommentIcon(onClick: () -> Unit) {
    val icon = painterResource(R.drawable.comment)
    IconButton(onClick = onClick) {
        Icon(icon, "comment", modifier = Modifier.size(24.dp), tint = Color(0xff424242))
    }
}

@Composable
fun HeartIcon(tint: Color = UnlikedPostColor, onClick: () -> Unit) {
    val icon = painterResource(R.drawable.heart)

    IconButton(onClick = onClick) {
        Icon(icon, "heart", modifier = Modifier.size(24.dp), tint = tint)
    }
}

val LikedPostColor = Color(0xffE61776)
val UnlikedPostColor = Color(0xff424242)

@Composable
fun RepostIcon(onClick: () -> Unit) {
    val icon = painterResource(R.drawable.repost)
    IconButton(onClick = onClick) {
        Icon(icon, "retweet", modifier = Modifier.size(24.dp), tint = Color(0xff424242))
    }
}

@Composable
fun ShareIcon(onClick: () -> Unit) {
    val icon = painterResource(R.drawable.share)
    IconButton(onClick = onClick) {
        Icon(icon, "share", modifier = Modifier.size(24.dp), tint = Color(0xff424242))
    }
}


@Composable
fun BookmarkIcon(onClick: () -> Unit) {
    val icon = painterResource(R.drawable.bookmark)
    IconButton(onClick = onClick) {
        Icon(icon, "share", modifier = Modifier.size(24.dp), tint = Color(0xff424242))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailUi(
    post: Post,
    modifier: Modifier = Modifier,
    selfUpdatingItemEventSink: (event: SelfUpdatingItem.Event<String, PostId, Post, Throwable>) -> Unit,
    eventSink: (event: PostDetailScreen.Event) -> Unit
) {

    println("^^^^${post}")

    val favoriteCount = post.favoriteCount

    println("^^^^FAVORITE COUNT = $favoriteCount")

    Column(modifier = modifier, verticalArrangement = Arrangement.SpaceBetween) {

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {

            TopAppBar(
                title = {
                    Text("Post")
                },
                navigationIcon = {
                    IconButton({
                        eventSink(PostDetailScreen.Event.GoBack)
                    }){
                        Icon(
                            painterResource(app.feed.common.R.drawable.arrow_left),
                            "arrow left",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))


            Row {

                Image(
                    painterResource(R.drawable.tag), "Tag", modifier = Modifier.size(60.dp).clip(
                        CircleShape
                    ),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text("Tag Ramotar", fontWeight = FontWeight.Bold)

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("@tag", color = Color(0xff424242))
                }


            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(post.text, color = Color.Black)

            Spacer(modifier = Modifier.height(16.dp))

            Text("#p${post.id.value} #g${1 + (10 - post.id.value.toInt().mod(10)).mod(10)}", color = Color(0xff229BF0))

            Spacer(modifier = Modifier.height(16.dp))

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
                    Text(favoriteCount.toString(), fontWeight = FontWeight.Bold)
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
                    CommentIcon {
                        // TODO
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RepostIcon {
                        // TODO
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    HeartIcon(tint = if (post.isLikedByViewer) LikedPostColor else UnlikedPostColor) {
                        val updatedPost = if (post.isLikedByViewer) {
                            post.copy(isLikedByViewer = false, favoriteCount = post.favoriteCount - 1)
                        } else {
                            post.copy(isLikedByViewer = true, favoriteCount = post.favoriteCount + 1)
                        }
                        selfUpdatingItemEventSink(SelfUpdatingItem.Event.Update(updatedPost))
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    BookmarkIcon {
                        // TODO
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    ShareIcon {
                        // TODO
                    }
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth().background(Color.White).padding(8.dp)) {
            TextField(
                value = "",
                placeholder = {
                    Text("Post your reply")
                },
                onValueChange = {},
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xfff3f3f3),
                    unfocusedContainerColor = Color(0xfff3f3f3),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                textStyle = MaterialTheme.typography.labelSmall,
                modifier = Modifier.fillMaxWidth().padding(2.dp),
                singleLine = true,
                shape = RoundedCornerShape(24.dp)
            )
        }


    }
}

