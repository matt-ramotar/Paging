package org.mobilenativefoundation.storex.paging.utils.timeline.models

import kotlinx.serialization.Serializable

@Serializable
data class GetFeedResponse(
    val posts: List<Post>,
    val nextCursor: PostId?,
)