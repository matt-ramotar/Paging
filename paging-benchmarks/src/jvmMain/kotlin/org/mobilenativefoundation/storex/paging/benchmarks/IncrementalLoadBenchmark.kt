package org.mobilenativefoundation.storex.paging.benchmarks

import androidx.paging.testing.asSnapshot
import kotlinx.benchmark.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.mobilenativefoundation.storex.paging.Pager
import org.mobilenativefoundation.storex.paging.PagingLoadState
import org.mobilenativefoundation.storex.paging.PagingRequest
import org.mobilenativefoundation.storex.paging.RecompositionMode
import org.mobilenativefoundation.storex.paging.internal.api.DispatcherProvider
import org.mobilenativefoundation.storex.paging.test.utils.TimelineAndroidxPagerFactory
import org.mobilenativefoundation.storex.paging.test.utils.TimelineAndroidxPagingSource
import org.mobilenativefoundation.storex.paging.test.utils.TimelinePagerFactory
import org.mobilenativefoundation.storex.paging.test.utils.models.GetFeedRequest
import org.mobilenativefoundation.storex.paging.test.utils.models.Post
import org.mobilenativefoundation.storex.paging.test.utils.models.PostId
import org.openjdk.jmh.annotations.Level

@Suppress("VisibleForTests")
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(BenchmarkTimeUnit.MILLISECONDS)
@Measurement(iterations = 10, time = 1000, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
class IncrementalLoadBenchmark {

    class KeyFactory(private val pageSize: Int) {
        fun nextCursor(prefetchDistance: Int, request: Int) =
            (prefetchDistance + 1 + (pageSize * (request - 1))).toString()

        fun nextKey(prefetchDistance: Int, request: Int) =
            GetFeedRequest(PostId(nextCursor(prefetchDistance, request)), pageSize)
    }

    private lateinit var coroutineDispatcher: CoroutineDispatcher
    private lateinit var requests: MutableSharedFlow<PagingRequest<GetFeedRequest>>
    private lateinit var storexPager: Pager<String, GetFeedRequest, Post, Throwable>
    private lateinit var androidxPager: androidx.paging.Pager<GetFeedRequest, Post>

    @Setup(Level.Invocation)
    fun setup(bh: Blackhole) {
        println("SETTING UP")
        val pageSize = 20
        val prefetchDistance = 100
        coroutineDispatcher = DispatcherProvider.io
        requests = MutableSharedFlow(replay = 1)
        storexPager = TimelinePagerFactory(
            pageSize = pageSize,
            prefetchDistance = prefetchDistance
        ).create(coroutineDispatcher, androidxPagingSourceFactory = { api ->
            TimelineAndroidxPagingSource(api, pageSize = pageSize)
        })
        androidxPager = TimelineAndroidxPagerFactory(pageSize, prefetchDistance).create()
    }

    @TearDown(Level.Invocation)
    fun tearDown(bh: Blackhole) = runBlocking {
        println("CLEANING UP")
        requests.emit(PagingRequest.invalidate())
    }


    @Benchmark
    fun storex1(bh: Blackhole) = runBlocking(coroutineDispatcher) {
        println("STARTING TEST")
        requests.emit(PagingRequest.enqueue(GetFeedRequest(PostId("101"), 20), jump = true))

        val flow = storexPager.pagingFlow(
            requests,
            recompositionMode = RecompositionMode.Immediate
        )

        flow.first {
            it.loadStates.append is PagingLoadState.NotLoading && it.ids.size == 120
        }
    }

    @Benchmark
    fun androidx1(bh: Blackhole) = runBlocking(coroutineDispatcher) {
        val items = androidxPager.flow.asSnapshot {
            scrollTo(20)
        }

        println("ITEMS = $items")

        items
    }

    @Benchmark
    fun storex2(bh: Blackhole) = runBlocking(coroutineDispatcher) {
        println("STARTING TEST")
        requests.emit(PagingRequest.enqueue(GetFeedRequest(PostId("101"), 20), jump = true))
        requests.emit(PagingRequest.enqueue(GetFeedRequest(PostId("121"), 20), jump = true))

        val flow = storexPager.pagingFlow(
            requests,
            recompositionMode = RecompositionMode.Immediate
        )

        flow.first {
            it.loadStates.append is PagingLoadState.NotLoading && it.ids.size == 140
        }
    }

    @Benchmark
    fun androidx2(bh: Blackhole) = runBlocking(coroutineDispatcher) {
        val items = androidxPager.flow.asSnapshot {
            scrollTo(40)
        }
        println("ITEMS = $items")
        items
    }
}