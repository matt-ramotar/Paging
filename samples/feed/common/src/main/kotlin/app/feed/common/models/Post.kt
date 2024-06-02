package app.feed.common.models

import android.os.Parcelable
import kotlinx.datetime.LocalDateTime
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import org.mobilenativefoundation.storex.paging.Identifiable

@Serializable
data class Post(
    override val id: PostId,
    val userId: String,
    val text: String,
    val createdAt: Long,
    val retweetCount: Int,
    val favoriteCount: Int,
    val commentCount: Int,
    val isRetweetedByViewer: Boolean,
    val isLikedByViewer: Boolean,
    val parentPostId: String? = null
) : Identifiable<String, PostId>


@Serializable
data class User(
    val id: String,
    val name: String,
    val profileImageUrl: String
)