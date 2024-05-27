package org.mobilenativefoundation.storex.paging.utils.timeline.models

data class Feed(
    val posts: List<Post>,
    val postsBefore: Int,
    val postsAfter: Int,
    val nextCursor: String?
)
