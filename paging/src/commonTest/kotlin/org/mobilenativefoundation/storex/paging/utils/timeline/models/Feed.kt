package org.mobilenativefoundation.storex.paging.utils.timeline.models

import kotlinx.serialization.Serializable

@Serializable
data class Feed(
    val posts: List<Post>,
    val postsBefore: Int,
    val postsAfter: Int,
    val nextCursor: PostId?
)
