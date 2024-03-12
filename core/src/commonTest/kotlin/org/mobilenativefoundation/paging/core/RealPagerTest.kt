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

    private val size = 10

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
            errorHandlingStrategy(ErrorHandlingStrategy.RetryLast())
        }
        .defaultLogger()

        .build()


    private suspend fun TurbineTestContext<PagingState<Id, K, P, D, E>>.testPrefetching() {

        fun checkRange(data: List<SD>) {
            data.forEachIndexed { index, item ->
                val id = index + 1
                assertEquals(id, item.id)
            }
        }

        val initial = awaitItem()
        assertIs<PagingState.Initial<Id, K, P, D, E>>(initial)

        val loading = awaitItem()
        assertIs<PagingState.Loading<Id, K, P, D, E>>(loading)

        // 1 - 10
        val idle1 = awaitItem()
        assertIs<PagingState.Data.Idle<Id, K, P, D, E>>(idle1)
        checkRange(idle1.data)
        assertEquals(size * 1, idle1.data.size)

        // 11 - 20
        val loadingMore11 = awaitItem()
        assertIs<PagingState.Data.LoadingMore<Id, K, P, D, E>>(loadingMore11)

        val idle11 = awaitItem()
        assertIs<PagingState.Data.Idle<Id, K, P, D, E>>(idle11)
        checkRange(idle11.data)
        assertEquals(size * 2, idle11.data.size)

        // 21 - 30
        val loadingMore21 = awaitItem()
        assertIs<PagingState.Data.LoadingMore<Id, K, P, D, E>>(loadingMore21)

        val idle21 = awaitItem()
        assertIs<PagingState.Data.Idle<Id, K, P, D, E>>(idle21)
        checkRange(idle21.data)
        assertEquals(size * 3, idle21.data.size)

        // 31 - 40
        val loadingMore31 = awaitItem()
        assertIs<PagingState.Data.LoadingMore<Id, K, P, D, E>>(loadingMore31)

        val idle31 = awaitItem()
        assertIs<PagingState.Data.Idle<Id, K, P, D, E>>(idle31)
        checkRange(idle31.data)
        assertEquals(size * 4, idle31.data.size)

        // 41 - 50
        val loadingMore41 = awaitItem()
        assertIs<PagingState.Data.LoadingMore<Id, K, P, D, E>>(loadingMore41)

        val idle41 = awaitItem()
        assertIs<PagingState.Data.Idle<Id, K, P, D, E>>(idle41)
        checkRange(idle41.data)
        assertEquals(size * 5, idle41.data.size)
    }

    @Test
    fun testPrefetching() = testScope.runTest {
        val initialKey: PK = PagingKey(0, TimelineKeyParams.Collection(size))
        val anchorPosition = MutableStateFlow(initialKey)
        val pager = TestPager(initialKey, anchorPosition)

        val state = pager.state

        state.test {
            this.testPrefetching()
            expectNoEvents()
        }
    }
}