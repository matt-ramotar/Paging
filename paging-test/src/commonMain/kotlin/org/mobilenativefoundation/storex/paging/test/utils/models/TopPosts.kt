package org.mobilenativefoundation.storex.paging.test.utils.models

import kotlinx.datetime.Clock
import org.mobilenativefoundation.storex.paging.ItemSnapshotList
import org.mobilenativefoundation.storex.paging.custom.Operation
import kotlin.time.Duration
import kotlin.time.Duration.Companion.INFINITE
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours


enum class TimeRange(val duration: Duration) {
    HOUR(1.hours),
    DAY(1.days),
    WEEK(7.days),
    MONTH(31.days),
    YEAR(365.days),
    INF(INFINITE)
}


class TopPosts(private val timeRange: TimeRange) :
    Operation<String, PostId, GetFeedRequest, Post>() {

    private fun isWithinRange(timestamp: Long?, range: TimeRange): Boolean {
        if (timestamp == null) return false

        val now = Clock.System.now().toEpochMilliseconds()
        val durationMillis = range.duration.inWholeMilliseconds
        return now - timestamp <= durationMillis
    }

    override operator fun invoke(snapshot: ItemSnapshotList<String, PostId, Post>): ItemSnapshotList<String, PostId, Post> {
        val filteredAndSorted = snapshot.filter { isWithinRange(it?.createdAt, timeRange) }
            .sortedByDescending { it?.favoriteCount }
        return ItemSnapshotList(filteredAndSorted)
    }
}
