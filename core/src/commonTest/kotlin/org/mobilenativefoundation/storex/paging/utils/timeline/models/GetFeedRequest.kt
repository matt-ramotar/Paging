package org.mobilenativefoundation.storex.paging.utils.timeline.models

data class GetFeedRequest(
    val cursor: String,
    val size: Int,
    val headers: MutableMap<String, String> = mutableMapOf()
)