package org.mobilenativefoundation.storex.paging.utils.timeline.models

import kotlinx.serialization.Serializable

@Serializable
data class GetFeedRequest(
    val cursor: PostId,
    val size: Int,
    val headers: MutableMap<String, String> = mutableMapOf()
)