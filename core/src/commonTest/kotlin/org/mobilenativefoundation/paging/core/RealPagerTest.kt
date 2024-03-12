package org.mobilenativefoundation.paging.core

import app.cash.turbine.TurbineTestContext
import app.cash.turbine.test
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.mobilenativefoundation.paging.core.impl.StorePagingSourceKeyFactory
import org.mobilenativefoundation.paging.core.utils.A
import org.mobilenativefoundation.paging.core.utils.Backend
import org.mobilenativefoundation.paging.core.utils.CK
import org.mobilenativefoundation.paging.core.utils.D
import org.mobilenativefoundation.paging.core.utils.E
import org.mobilenativefoundation.paging.core.utils.Id
import org.mobilenativefoundation.paging.core.utils.K
import org.mobilenativefoundation.paging.core.utils.P
import org.mobilenativefoundation.paging.core.utils.PD
import org.mobilenativefoundation.paging.core.utils.PK
import org.mobilenativefoundation.paging.core.utils.SD
import org.mobilenativefoundation.paging.core.utils.TimelineKeyParams
import org.mobilenativefoundation.paging.core.utils.TimelineStoreFactory
import org.mobilenativefoundation.store.store5.ExperimentalStoreApi
import org.mobilenativefoundation.store.store5.MutableStore
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@Suppress("TestFunctionName")
@OptIn(ExperimentalStoreApi::class)
class RealPagerTest {
    private val testScope = TestScope()

    private lateinit var backend: Backend
    private lateinit var timelineStoreFactory: TimelineStoreFactory
    private lateinit var timelineStore: MutableStore<PK, PD>

    @BeforeTest
    fun setup() {
        backend = Backend()
        timelineStoreFactory = TimelineStoreFactory(backend.feedService, backend.postService)
        timelineStore = timelineStoreFactory.create()
    }

    private fun TestScope.TestPager(
        initialKey: PK,
        anchorPosition: StateFlow<PK>,
        pagingConfig: PagingConfig = PagingConfig(10, prefetchDistance = 50, insertionStrategy = InsertionStrategy.APPEND),
        maxRetries: Int = 3,
        errorHandlingStrategy: ErrorHandlingStrategy = ErrorHandlingStrategy.RetryLast(maxRetries)
    ): Pager<Id, K, P, D, E, A> = PagerBuilder<Id, K, P, D, E, A>(
        scope = this,
        initialKey = initialKey,
        initialState = PagingState.Initial(initialKey, null),
        anchorPosition = anchorPosition
    )
        .pagingConfig(pagingConfig)

        .mutableStorePagingSource(timelineStore) {
            StorePagingSourceKeyFactory {
                PagingKey(it.id, TimelineKeyParams.Single)
            }
        }

        .defaultReducer {
            errorHandlingStrategy(errorHandlingStrategy)
        }
        .defaultLogger()

        .build()


    private suspend fun TurbineTestContext<PagingState<Id, K, P, D, E>>.verifyPrefetching(
        pageSize: Int,
        prefetchDistance: Int
    ) {
        fun checkRange(data: List<SD>) {
            data.forEachIndexed { index, item ->
                val id = index + 1
                assertEquals(id, item.id)
            }
        }

        val initial = awaitItem()
        assertIs<PagingState.Initial<Id, K, P, D, E>>(initial)

        if (prefetchDistance > 0) {
            val loading = awaitItem()
            assertIs<PagingState.Loading<Id, K, P, D, E>>(loading)

            val idle = awaitItem()
            assertIs<PagingState.Data.Idle<Id, K, P, D, E>>(idle)
            checkRange(idle.data)
            assertEquals(pageSize, idle.data.size)
        }

        var currentPage = 2
        var expectedDataSize = pageSize

        while (expectedDataSize < prefetchDistance) {
            val loadingMore = awaitItem()
            assertIs<PagingState.Data.LoadingMore<Id, K, P, D, E>>(loadingMore)

            val idle = awaitItem()
            assertIs<PagingState.Data.Idle<Id, K, P, D, E>>(idle)
            checkRange(idle.data)
            expectedDataSize += pageSize
            assertEquals(expectedDataSize, idle.data.size)

            currentPage++
        }
    }

    @Test
    fun testPrefetchingWhenPrefetchDistanceIsGreaterThan0() = testScope.runTest {
        val pageSize = 10
        val prefetchDistance = 50
        val initialKey: PK = PagingKey(0, TimelineKeyParams.Collection(pageSize))
        val anchorPosition = MutableStateFlow(initialKey)
        val pager = TestPager(initialKey, anchorPosition, pagingConfig = PagingConfig(pageSize, prefetchDistance, InsertionStrategy.APPEND))

        val state = pager.state

        state.test {
            verifyPrefetching(pageSize, prefetchDistance)
            expectNoEvents()
        }
    }

    @Test
    fun testPrefetchingWhenPrefetchDistanceEquals0() = testScope.runTest {
        val pageSize = 10
        val prefetchDistance = 0
        val initialKey: PK = PagingKey(0, TimelineKeyParams.Collection(pageSize))
        val anchorPosition = MutableStateFlow(initialKey)
        val pager = TestPager(initialKey, anchorPosition, pagingConfig = PagingConfig(pageSize, prefetchDistance, InsertionStrategy.APPEND))

        val state = pager.state

        state.test {
            verifyPrefetching(pageSize, prefetchDistance)
            expectNoEvents()
        }
    }

    @Test
    fun testUserLoadWhenPrefetchDistanceEquals0() = testScope.runTest {
        val pageSize = 10
        val prefetchDistance = 0
        val initialKey: PK = PagingKey(0, TimelineKeyParams.Collection(pageSize))
        val anchorPosition = MutableStateFlow(initialKey)
        val pager = TestPager(initialKey, anchorPosition, pagingConfig = PagingConfig(pageSize, prefetchDistance, InsertionStrategy.APPEND))

        val state = pager.state

        state.test {
            verifyPrefetching(pageSize, prefetchDistance)

            pager.dispatch(PagingAction.User.Load(initialKey))

            val loading = awaitItem()
            assertIs<PagingState.Loading<Id, K, P, D, E>>(loading)

            val idle = awaitItem()
            assertIs<PagingState.Data.Idle<Id, K, P, D, E>>(idle)
            assertEquals(pageSize, idle.data.size)

            expectNoEvents()
        }
    }

    @Test
    fun testErrorHandlingStrategyRetryLast() = testScope.runTest {
        val pageSize = 10
        val prefetchDistance = 0
        val initialKey: CK = PagingKey(0, TimelineKeyParams.Collection(pageSize))
        val anchorPosition = MutableStateFlow(initialKey)
        val maxRetries = 3

        val message = "Failed to load data"
        val throwable = Throwable(message)
        backend.failWith(throwable)

        val pager = TestPager(initialKey, anchorPosition, pagingConfig = PagingConfig(pageSize, prefetchDistance, InsertionStrategy.APPEND), maxRetries = maxRetries)

        val state = pager.state

        state.test {
            val initial = awaitItem()
            assertIs<PagingState.Initial<Id, K, P, D, E>>(initial)

            pager.dispatch(PagingAction.User.Load(initialKey))

            val loading = awaitItem()
            assertIs<PagingState.Loading<Id, K, P, D, E>>(loading)

            val error = awaitItem()
            assertIs<PagingState.Error.Exception<Id, K, P, D, E>>(error)
            assertEquals(throwable, error.error)

            val retryCount = backend.getRetryCountFor(initialKey)
            assertEquals(maxRetries, retryCount)

            expectNoEvents()
        }
    }

    @Test
    fun testErrorHandlingStrategyPassThrough() = testScope.runTest {
        val pageSize = 10
        val prefetchDistance = 0
        val initialKey: CK = PagingKey(0, TimelineKeyParams.Collection(pageSize))
        val anchorPosition = MutableStateFlow(initialKey)


        val message = "Failed to load data"
        val throwable = Throwable(message)
        backend.failWith(throwable)

        val pager = TestPager(initialKey, anchorPosition, pagingConfig = PagingConfig(pageSize, prefetchDistance, InsertionStrategy.APPEND), errorHandlingStrategy = ErrorHandlingStrategy.PassThrough)

        val state = pager.state

        state.test {
            val initial = awaitItem()
            assertIs<PagingState.Initial<Id, K, P, D, E>>(initial)

            pager.dispatch(PagingAction.User.Load(initialKey))

            val loading = awaitItem()
            assertIs<PagingState.Loading<Id, K, P, D, E>>(loading)

            val error = awaitItem()
            assertIs<PagingState.Error.Exception<Id, K, P, D, E>>(error)
            assertEquals(throwable, error.error)
            val retryCount = backend.getRetryCountFor(initialKey)
            assertEquals(0, retryCount)

            expectNoEvents()
        }
    }
}