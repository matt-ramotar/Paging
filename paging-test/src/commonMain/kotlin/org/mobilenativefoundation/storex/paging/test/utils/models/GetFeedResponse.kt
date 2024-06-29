package org.mobilenativefoundation.storex.paging.test.utils.models

import kotlinx.serialization.Serializable

@Serializable
data class GetFeedResponse(
    val posts: List<Post>,
    val nextCursor: PostId?,
)