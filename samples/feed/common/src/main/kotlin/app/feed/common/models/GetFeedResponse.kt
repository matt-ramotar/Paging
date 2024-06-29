package app.feed.common.models

import kotlinx.serialization.Serializable

@Serializable
data class GetFeedResponse(
    val posts: List<Post>,
    val nextCursor: PostId?,
    val prevCursor: PostId?,
)