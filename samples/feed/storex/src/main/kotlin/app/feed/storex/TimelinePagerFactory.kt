package app.feed.storex


import app.feed.common.api.TimelineApi
import app.feed.common.models.GetFeedRequest
import app.feed.common.models.Post
import app.feed.common.models.PostId
import app.feed.common.server.Server
import kotlinx.coroutines.CoroutineDispatcher
import org.mobilenativefoundation.storex.paging.Pager
import org.mobilenativefoundation.storex.paging.PagingConfig
import org.mobilenativefoundation.storex.paging.PagingSource
import org.mobilenativefoundation.storex.paging.internal.api.DispatcherProvider


class TimelinePagerFactory(
    private val pageSize: Int = 20,
    private val prefetchDistance: Int = 100,
) {

    private val server = Server()
    private val api = TimelineApi(server)

    fun create(
        coroutineDispatcher: CoroutineDispatcher = DispatcherProvider.io,
        storexPagingSourceFactory: ((api: TimelineApi) -> org.mobilenativefoundation.storex.paging.PagingSource<String, GetFeedRequest, Post, Throwable>)? = null,
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

            val nextKey = response.nextCursor?.let { nextCursor ->
                params.key.copy(
                    cursor = nextCursor,
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
