package org.mobilenativefoundation.storex.paging.utils.timeline.models

data class GetPostRequest(
    val id: String,
    val headers: MutableMap<String, String> = mutableMapOf()
)