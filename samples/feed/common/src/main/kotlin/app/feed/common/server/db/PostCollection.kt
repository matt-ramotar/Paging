package app.feed.common.server.db

import app.feed.common.models.Post
import app.feed.common.models.PostId
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant


fun createdAt(month: Month, day: Int): LocalDateTime {
    val year = 2024
    val hour = 10
    val minute = 0
    val second = 0

    return LocalDateTime(year, month, day, hour, minute, second)
}

fun distributeIdsToDates(ids: List<Int>, createdAtDates: List<LocalDateTime>): Map<LocalDateTime, List<Int>> {
    val groupedIds = mutableMapOf<LocalDateTime, MutableList<Int>>()

    for (date in createdAtDates) {
        groupedIds[date] = mutableListOf()
    }

    val groupSize = ids.size / createdAtDates.size
    for ((index, id) in ids.withIndex()) {
        val dateIndex = index / groupSize
        val date = createdAtDates[dateIndex]
        groupedIds[date]?.add(id)
    }

    return groupedIds
}

class PostCollection {
    private val allIds: MutableList<PostId> = mutableListOf()
    private val byId: MutableMap<PostId, Post> = mutableMapOf()

    val userIds = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
    val createdAtDates = listOf(
        createdAt(Month.MAY, 29),
        createdAt(Month.MAY, 30),
        createdAt(Month.MAY, 31),
        createdAt(Month.JUNE, 1),
        createdAt(Month.JUNE, 2),
        createdAt(Month.JUNE, 3),
        createdAt(Month.JUNE, 4),
        createdAt(Month.JUNE, 5),
        createdAt(Month.JUNE, 6),
        createdAt(Month.JUNE, 7),
    )

    val ids = (0..499).toList()

    val groupedIds = distributeIdsToDates(ids, createdAtDates)


    val idToDateMap = mutableMapOf<Int, Long>()


    init {

        for ((date, idList) in groupedIds) {
            for (id in idList) {
                idToDateMap[id] = date.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
            }
        }

        ids.map { index ->
            val id = (index + 1).toString()
            val postId = PostId(id)
            allIds.add(postId)
            val userId = userIds[index.mod(10)]
            byId[postId] = Post(
                id = postId,
                userId = userId.toString(),
                text = "Woofsem woofor sit amet, consectetur woofipiscing elit. Woofellus woofrerit sed woofus commodo wooferdum.",
                createdAt = idToDateMap[index]!!,
                retweetCount = 0,
                favoriteCount = 0,
                commentCount = 0,
                isRetweetedByViewer = false,
                isLikedByViewer = false,
                parentPostId = null
            )
        }
    }

    fun getAll(): List<Post> {
        return allIds.mapNotNull { byId[it] }
    }

    fun getById(id: PostId): Post? {
        return byId[id]
    }

    fun addAll(index: Int, posts: List<Post>) {
        allIds.addAll(index, posts.map { it.id })
        byId.putAll(posts.associateBy { it.id })
    }
}