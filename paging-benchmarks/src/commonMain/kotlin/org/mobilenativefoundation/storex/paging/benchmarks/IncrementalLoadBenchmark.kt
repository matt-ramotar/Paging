package org.mobilenativefoundation.storex.paging.benchmarks

import kotlinx.benchmark.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import org.mobilenativefoundation.storex.paging.Pager
import org.mobilenativefoundation.storex.paging.PagingLoadState
import org.mobilenativefoundation.storex.paging.PagingRequest
import org.mobilenativefoundation.storex.paging.RecompositionMode
import org.mobilenativefoundation.storex.paging.internal.api.DispatcherProvider
import org.mobilenativefoundation.storex.paging.test.utils.TimelineAndroidxPagingSource
import org.mobilenativefoundation.storex.paging.test.utils.models.GetFeedRequest
import org.mobilenativefoundation.storex.paging.test.utils.models.Post
import org.mobilenativefoundation.storex.paging.test.utils.models.PostId


@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(BenchmarkTimeUnit.MILLISECONDS)
class IncrementalLoadBenchmark {

    class KeyFactory(private val pageSize: Int) {
        fun nextCursor(prefetchDistance: Int, request: Int) =
            (prefetchDistance + 1 + (pageSize * (request - 1))).toString()

        fun nextKey(prefetchDistance: Int, request: Int) =
            GetFeedRequest(PostId(nextCursor(prefetchDistance, request)), pageSize)

    }

    val pageSize = 20
    val prefetchDistance = 100

    lateinit var pager: Pager<String, GetFeedRequest, Post, Throwable>

    @Setup
    fun setup() {


        val coroutineDispatcher = DispatcherProvider.io


        pager = org.mobilenativefoundation.storex.paging.test.utils.TimelinePagerFactory(
            pageSize = pageSize,
            prefetchDistance = prefetchDistance
        )
            .create(coroutineDispatcher, androidxPagingSourceFactory = { api ->
                TimelineAndroidxPagingSource(api, pageSize = pageSize)
            })

    }

    @Benchmark
    fun skipQueueStoreX(bh: Blackhole) = runBlocking {

        val keyFactory = KeyFactory(pageSize)

        val requests = MutableSharedFlow<PagingRequest<GetFeedRequest>>()

        requests.emit(PagingRequest.enqueue(keyFactory.nextKey(prefetchDistance, 1)))

        pager.pagingFlow(requests, RecompositionMode.Immediate).firstOrNull {
            it.loadStates.append is PagingLoadState.NotLoading && it.ids.size == 120
        }

    }


}