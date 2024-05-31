package org.mobilenativefoundation.storex.paging.benchmarks


import androidx.paging.testing.asSnapshot
import kotlinx.benchmark.*
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import org.mobilenativefoundation.storex.paging.PagingLoadState
import org.mobilenativefoundation.storex.paging.internal.api.DispatcherProvider
import org.mobilenativefoundation.storex.paging.pagingFlow
import org.mobilenativefoundation.storex.paging.test.utils.TimelineAndroidxPagerFactory
import org.mobilenativefoundation.storex.paging.test.utils.TimelineAndroidxPagingSource

//@State(Scope.Benchmark)
//@BenchmarkMode(Mode.AverageTime)
//@OutputTimeUnit(BenchmarkTimeUnit.MILLISECONDS)
//class TestBenchmark {
//
//
//    @Benchmark
//    fun eagerLoading100StoreX(bh: Blackhole) = runBlocking {
//
//        val coroutineDispatcher = DispatcherProvider.io
//
//        val pageSize = 20
//        val prefetchDistance = 100
//
//        val pager =
//            org.mobilenativefoundation.storex.paging.test.utils.TimelinePagerFactory(
//                pageSize = pageSize,
//                prefetchDistance = prefetchDistance
//            )
//                .create(coroutineDispatcher, androidxPagingSourceFactory = { api ->
//                    TimelineAndroidxPagingSource(api, pageSize = pageSize)
//                })
//
//        pager.pagingFlow().firstOrNull {
//            it.loadStates.append is PagingLoadState.NotLoading && it.ids.size == 100
//        }
//
//    }
//
//    @Benchmark
//    fun eagerLoading100AndroidX(bh: Blackhole) = runBlocking {
//
//        val pageSize = 20
//        val prefetchDistance = 80
//
//
//        TimelineAndroidxPagerFactory(pageSize, prefetchDistance).create().flow.asSnapshot()
//    }
//
//
//}