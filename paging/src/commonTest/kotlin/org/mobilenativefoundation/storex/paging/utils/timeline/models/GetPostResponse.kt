package org.mobilenativefoundation.storex.paging.utils.timeline.models

import kotlinx.serialization.Serializable

@Serializable
data class GetPostResponse(
    val post: Post?
)