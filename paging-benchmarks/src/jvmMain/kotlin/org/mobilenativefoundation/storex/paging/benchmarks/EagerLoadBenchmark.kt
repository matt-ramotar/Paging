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
import org.mobilenativefoundation.storex.paging.internal.api.DispatcherProvider
import org.mobilenativefoundation.storex.paging.launchPagingFlow
import org.mobilenativefoundation.storex.paging.test.utils.TimelineAndroidxPagerFactory
import org.mobilenativefoundation.storex.paging.test.utils.TimelineAndroidxPagingSource
import org.mobilenativefoundation.storex.paging.test.utils.TimelinePagerFactory
import org.mobilenativefoundation.storex.paging.test.utils.models.GetFeedRequest
import org.mobilenativefoundation.storex.paging.test.utils.models.Post
import org.openjdk.jmh.annotations.Level
import org.openjdk.jmh.annotations.State

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(BenchmarkTimeUnit.MILLISECONDS)
@Measurement(iterations = 10, time = 1000, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
class EagerLoadBenchmark {

    private val pageSize = 20
    private val prefetchDistance = 100

    private lateinit var coroutineDispatcher: CoroutineDispatcher
    private lateinit var requests: MutableSharedFlow<PagingRequest<GetFeedRequest>>
    private lateinit var storexPager: Pager<String, GetFeedRequest, Post, Throwable>
    private lateinit var androidxPager: androidx.paging.Pager<GetFeedRequest, Post>

    @Setup(Level.Invocation)
    fun setup(bh: Blackhole) {
        println("SETTING UP")
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
    fun storex100(bh: Blackhole) = runBlocking(coroutineDispatcher) {
        storexPager.launchPagingFlow().first {
            it.loadStates.append is PagingLoadState.NotLoading && it.ids.size == 100
        }
    }

    @Suppress("VisibleForTests")
    @Benchmark
    fun androidx100(bh: Blackhole) = runBlocking(coroutineDispatcher) {
        val items = androidxPager.flow.asSnapshot()
        println("ANDROIDX ITEMS = ${items.size}")
        items
    }
}