package org.mobilenativefoundation.paging.core.utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.mobilenativefoundation.paging.core.Effect
import org.mobilenativefoundation.paging.core.Middleware
import org.mobilenativefoundation.paging.core.PagingAction
import org.mobilenativefoundation.paging.core.PagingData
import org.mobilenativefoundation.paging.core.PagingKey
import org.mobilenativefoundation.paging.core.PagingSource
import org.mobilenativefoundation.paging.core.PagingState
import org.mobilenativefoundation.paging.core.UserCustomActionReducer
import org.mobilenativefoundation.store.store5.Converter
import org.mobilenativefoundation.store.store5.ExperimentalStoreApi
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.MutableStore
import org.mobilenativefoundation.store.store5.StoreBuilder
import org.mobilenativefoundation.store.store5.Updater
import org.mobilenativefoundation.store.store5.UpdaterResult
import kotlin.math.max

typealias Id = Int
typealias K = Int
typealias P = TimelineKeyParams
typealias CP = TimelineKeyParams.Collection
typealias SP = TimelineKeyParams.Single
typealias PK = PagingKey<Id, P>
typealias SK = PagingKey<Id, SP>
typealias CK = PagingKey<Id, CP>
typealias D = TimelineData
typealias PD = PagingData<Id, K, P, TimelineData>
typealias CD = PagingData.Collection<Int, Int, P, TimelineData>
typealias SD = PagingData.Single<Int, Int, P, TimelineData>
typealias A = TimelineAction
typealias E = TimelineError

sealed class TimelineError {
    data class Exception(val throwable: Throwable) : TimelineError()
}

sealed interface TimelineAction {
    data object ClearData : TimelineAction
}

class TimelineActionReducer : UserCustomActionReducer<Id, K, P, D, E, A> {
    override fun reduce(action: PagingAction.User.Custom<Id, K, P, D, E, A>, state: PagingState<Id, K, P, D, E>): PagingState<Id, K, P, D, E> {
        return when (action.action) {
            TimelineAction.ClearData -> {
                val nextState = when (state) {
                    is PagingState.Data.ErrorLoadingMore<Id, K, P, D, E, *> -> state.copy(data = emptyList())
                    is PagingState.Data.Idle -> state.copy(data = emptyList())
                    is PagingState.Data.LoadingMore -> state.copy(data = emptyList())
                    is PagingState.Error.Custom,
                    is PagingState.Error.Exception,
                    is PagingState.Initial,
                    is PagingState.Loading -> state
                }

                nextState
            }
        }
    }

}

class AuthMiddleware(private val authTokenProvider: () -> String) : Middleware<Id, K, P, D, E, A> {
    private fun setAuthToken(headers: MutableMap<String, String>) = headers.apply {
        this["auth"] = authTokenProvider()
    }

    override suspend fun apply(action: PagingAction<Id, K, P, D, E, A>, next: suspend (PagingAction<Id, K, P, D, E, A>) -> Unit) {
        when (action) {
            is PagingAction.User.Load -> {
                setAuthToken(action.key.params.headers)
                next(action)
            }

            is PagingAction.Load -> {
                setAuthToken(action.key.params.headers)
                next(action)
            }

            else -> next(action)
        }
    }
}

class ErrorLoggingEffect(private val log: (error: E) -> Unit) : Effect<Id, K, P, D, E, A, PagingAction.UpdateError<Id, K, P, D, E, A>, PagingState.Error.Exception<Id, K, P, D, E>> {
    override fun invoke(action: PagingAction.UpdateError<Id, K, P, D, E, A>, state: PagingState.Error.Exception<Id, K, P, D, E>, dispatch: (PagingAction<Id, K, P, D, E, A>) -> Unit) {
        when (val error = action.error) {
            is PagingSource.LoadResult.Error.Custom -> {}
            is PagingSource.LoadResult.Error.Exception -> {
                log(TimelineError.Exception(error.error))
            }
        }
    }
}

enum class KeyType {
    SINGLE,
    COLLECTION
}


/**
 * An enum defining sorting options that can be applied during fetching.
 */
enum class Sort {
    NEWEST,
    OLDEST,
    ALPHABETICAL,
    REVERSE_ALPHABETICAL,
}

/**
 * Defines filters that can be applied during fetching.
 */
interface Filter<T : Any> {
    operator fun invoke(items: List<T>): List<T>
}


sealed interface TimelineKeyParams {
    val headers: MutableMap<String, String>

    data class Single(
        override val headers: MutableMap<String, String> = mutableMapOf(),
    ) : TimelineKeyParams

    data class Collection(
        val size: Int,
        val filter: List<Filter<SD>> = emptyList(),
        val sort: Sort? = null,
        override val headers: MutableMap<String, String> = mutableMapOf()
    ) : TimelineKeyParams
}

sealed class TimelineData {
    data class Post(
        val id: Id,
        val content: String
    ) : TimelineData()

    data class Feed(
        val posts: List<Post>,
        val itemsBefore: Int,
        val itemsAfter: Int,
        val nextKey: PK?
    ) : TimelineData()
}

interface FeedService {
    suspend fun get(key: CK): TimelineData.Feed
}

interface PostService {
    suspend fun get(key: SK): TimelineData.Post?
    suspend fun update(key: SK, value: TimelineData.Post)
}

class RealFeedService(
    private val posts: List<TimelineData.Post>,
    private val error: StateFlow<Throwable?>,
    private val incrementTriesFor: (key: CK) -> Unit,
    private val setHeaders: (key: CK) -> Unit
) : FeedService {

    override suspend fun get(key: CK): TimelineData.Feed {
        setHeaders(key)

        error.value?.let {
            incrementTriesFor(key)
            throw it
        }

        val start = key.key
        val end = start + key.params.size
        val posts = this.posts.subList(start, end)

        return TimelineData.Feed(
            posts,
            itemsBefore = start - 1,
            itemsAfter = this.posts.size - end,
            nextKey = PagingKey(end, key.params)
        )
    }
}

class RealPostService(
    private val posts: MutableMap<SK, TimelineData.Post>,
    private val error: StateFlow<Throwable?>
) : PostService {
    override suspend fun get(key: SK): TimelineData.Post? {
        error.value?.let { throw it }

        return posts[key]
    }

    override suspend fun update(key: SK, value: TimelineData.Post) {
        error.value?.let { throw it }

        posts[key] = value
    }

}


data class Event(
    val name: String,
    val message: String
)

class Backend {

    private val posts = mutableMapOf<SK, TimelineData.Post>()
    private val error = MutableStateFlow<Throwable?>(null)
    private val tries: MutableMap<CK, Int> = mutableMapOf()
    private val logs = mutableListOf<Event>()

    private val headers: MutableMap<CK, MutableMap<String, String>> = mutableMapOf()

    init {
        (1..200).map { TimelineData.Post(it, "Post $it") }.forEach { this.posts[PagingKey(it.id, TimelineKeyParams.Single())] = it }
    }

    val feedService: FeedService = RealFeedService(posts.values.toList(), error, { key ->
        if (key !in tries) {
            tries[key] = 0
        }

        tries[key] = tries[key]!! + 1
    }, { key ->
        if (key !in headers) {
            headers[key] = key.params.headers
        }

        val mergedHeaders = headers[key]!! + key.params.headers

        headers[key] = mergedHeaders.toMutableMap()
    })

    val postService: PostService = RealPostService(posts, error)

    fun failWith(error: Throwable) {
        this.error.value = error
    }

    fun clearError() {
        this.error.value = null
    }

    fun getRetryCountFor(key: CK): Int {
        val tries = tries[key] ?: 0
        val retries = tries - 1
        return max(retries, 0)
    }

    fun getHeadersFor(key: CK): Map<String, String> {
        val headers = this.headers[key] ?: mapOf()
        return headers
    }

    fun log(name: String, message: String) {
        logs.add(Event(name, message))
    }

    fun getLogs() = logs
}


@OptIn(ExperimentalStoreApi::class)
class TimelineStoreFactory(
    private val feedService: FeedService,
    private val postService: PostService,
) {

    private fun createFetcher(): Fetcher<PK, PD> = Fetcher.of { key ->


        when (val params = key.params) {
            is TimelineKeyParams.Collection -> {
                val ck = PagingKey(key.key, params)
                val feed = feedService.get(ck)
                PagingData.Collection(
                    items = feed.posts.map { post -> PagingData.Single(post.id, post) },
                    itemsBefore = feed.itemsBefore,
                    itemsAfter = feed.itemsAfter,
                    prevKey = key,
                    nextKey = feed.nextKey
                )
            }

            is TimelineKeyParams.Single -> {
                val sk = PagingKey(key.key, params)
                val post = postService.get(sk)
                if (post == null) {
                    throw Throwable("Post is null")
                } else {
                    PagingData.Single(post.id, post)
                }
            }
        }
    }

    private fun createConverter(): Converter<PD, PD, PD> = Converter.Builder<PD, PD, PD>()
        .fromOutputToLocal { it }
        .fromNetworkToLocal { it }
        .build()

    private fun createUpdater(): Updater<PK, PD, Any> = Updater.by(
        post = { key, value ->
            when (val params = key.params) {
                is TimelineKeyParams.Single -> {
                    if (value is PagingData.Single) {
                        val updatedValue = value.data
                        if (updatedValue is TimelineData.Post) {
                            val sk = PagingKey(key.key, params)
                            val response = postService.update(sk, updatedValue)
                            UpdaterResult.Success.Typed(response)
                        } else {
                            UpdaterResult.Error.Message("Updated value is the wrong type. Expected ${TimelineData.Post::class}, received ${updatedValue::class}")
                        }
                    } else {
                        UpdaterResult.Error.Message("Updated value is the wrong type. Expected ${PagingData.Single::class}, received ${value::class}")
                    }
                }

                is TimelineKeyParams.Collection -> throw UnsupportedOperationException("Updating collections is not supported")
            }
        },
    )

    fun create(): MutableStore<PK, PD> =
        StoreBuilder.from(
            fetcher = createFetcher()
        ).toMutableStoreBuilder(
            converter = createConverter()
        ).build(
            updater = createUpdater(),
            bookkeeper = null
        )
}