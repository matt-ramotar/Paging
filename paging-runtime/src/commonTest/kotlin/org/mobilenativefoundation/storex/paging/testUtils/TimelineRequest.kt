package org.mobilenativefoundation.storex.paging.testUtils

data class TimelineRequest(
    val cursor: String?,
    val size: Int = 20,
    val headers: Map<String, String> = emptyMap()
) : Comparable<TimelineRequest> {
    override fun compareTo(other: TimelineRequest): Int {
        return if (cursor != null && other.cursor != null) {
            cursor.compareTo(other.cursor)
        } else if (cursor != null) {
            1
        } else if (other.cursor != null) {
            -1
        } else {
            0
        }
    }
}
