package org.mobilenativefoundation.storex.paging.utils.timeline.models

data class GetFeedResponse(
    val posts: List<Post>,
    val nextCursor: String?,
)