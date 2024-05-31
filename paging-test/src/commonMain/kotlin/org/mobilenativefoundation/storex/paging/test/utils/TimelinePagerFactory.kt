package org.mobilenativefoundation.storex.paging.test.utils

import kotlinx.coroutines.CoroutineDispatcher
import org.mobilenativefoundation.storex.paging.Pager
import org.mobilenativefoundation.storex.paging.PagingConfig
import org.mobilenativefoundation.storex.paging.PagingSource
import org.mobilenativefoundation.storex.paging.test.utils.api.TimelineApi
import org.mobilenativefoundation.storex.paging.test.utils.models.GetFeedRequest
import org.mobilenativefoundation.storex.paging.test.utils.models.Post
import org.mobilenativefoundation.storex.paging.test.utils.models.PostId
import org.mobilenativefoundation.storex.paging.utils.timeline.server.Server

class TimelinePagerFactory(
    private val pageSize: Int = 20,
    private val prefetchDistance: Int = 100,
) {

    private val server = Server()
    private val api = TimelineApi(server)

    fun create(
        coroutineDispatcher: CoroutineDispatcher,
        storexPagingSourceFactory: ((api: TimelineApi) -> PagingSource<String, GetFeedRequest, Post, Throwable>)? = null,
        androidxPagingSourceFactory: ((api: TimelineApi) -> androidx.paging.PagingSource<GetFeedRequest, Post>)? = null,
    ): Pager<String, GetFeedRequest, Post, Throwable> {

        val builder = Pager.Builder<String, GetFeedRequest, Post>(
            pagingConfig = PagingConfig(
                placeholderId = PostId.Placeholder,
                initialKey = GetFeedRequest(PostId("1"), pageSize),
                prefetchDistance = prefetchDistance
            )
        ).coroutineDispatcher(coroutineDispatcher)

        if (storexPagingSourceFactory != null) {
            builder.storexPagingSource(storexPagingSourceFactory(api))
        } else if (androidxPagingSourceFactory != null) {
            builder.androidxPagingSource(androidxPagingSourceFactory(api))
        } else {
            builder.storexPagingSource(storexPagingSource())
        }

        return builder.build()
    }


    private fun storexPagingSource() =
        PagingSource<String, GetFeedRequest, Post, Throwable> { params ->
            val response = api.getFeed(params.key)

            val nextKey = response.nextCursor?.let {
                params.key.copy(
                    cursor = response.nextCursor,
                    size = pageSize
                )
            }
            println("NEXT KEY = $nextKey")
            PagingSource.LoadResult.Data(
                items = response.posts,
                params = params,
                prevKey = getPreviousCursor(params.key.cursor)?.let { params.key.copy(cursor = it) },
                nextKey = nextKey,
                origin = PagingSource.LoadResult.Data.Origin.Network,

                )
        }

    private val cursorHistory = LinkedHashMap<PostId, PostId>()

    private fun getPreviousCursor(currentCursor: PostId): PostId? {
        val prevCursor = cursorHistory[currentCursor]
        if (prevCursor != null) {
            cursorHistory.remove(currentCursor)
        }
        return prevCursor
    }
}
