package app.feed.common

import androidx.paging.PagingSource
import androidx.paging.PagingState
import app.feed.common.api.TimelineApi
import app.feed.common.models.GetFeedRequest
import app.feed.common.models.Post
import app.feed.common.models.PostId

class TimelineAndroidxPagingSource(
    private val api: TimelineApi,
    private val pageSize: Int
) : PagingSource<GetFeedRequest, Post>() {

    private val cursorHistory = LinkedHashMap<PostId, PostId>()

    override fun getRefreshKey(state: PagingState<GetFeedRequest, Post>): GetFeedRequest? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey ?: anchorPage?.nextKey
        }
    }

    override suspend fun load(params: LoadParams<GetFeedRequest>): LoadResult<GetFeedRequest, Post> {
        return try {
            val key = params.key ?: GetFeedRequest(PostId("1"), pageSize)
            val response = api.getFeed(key)

            val nextKey = response.nextCursor?.let {
                key.copy(cursor = response.nextCursor, size = pageSize)
            }

            LoadResult.Page(
                data = response.posts,
                prevKey = getPreviousCursor(key.cursor)?.let { key.copy(cursor = it) },
                nextKey = nextKey
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    private fun getPreviousCursor(currentCursor: PostId?): PostId? {
        if (currentCursor == null) {
            return null
        }

        val prevCursor = cursorHistory[currentCursor]
        if (prevCursor != null) {
            cursorHistory.remove(currentCursor)
        }
        return prevCursor
    }
}