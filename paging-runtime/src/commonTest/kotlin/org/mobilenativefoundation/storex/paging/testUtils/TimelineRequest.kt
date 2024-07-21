package org.mobilenativefoundation.storex.paging.testUtils

data class TimelineRequest(
    val cursor: String?,
    val size: Int = 20,
    val headers: Map<String, String> = emptyMap()
)