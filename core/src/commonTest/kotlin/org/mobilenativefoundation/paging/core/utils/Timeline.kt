package org.mobilenativefoundation.paging.core.utils

import org.mobilenativefoundation.paging.core.PagingData
import org.mobilenativefoundation.paging.core.PagingKey
import org.mobilenativefoundation.store.store5.Converter
import org.mobilenativefoundation.store.store5.ExperimentalStoreApi
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.MutableStore
import org.mobilenativefoundation.store.store5.StoreBuilder
import org.mobilenativefoundation.store.store5.Updater
import org.mobilenativefoundation.store.store5.UpdaterResult

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

sealed class TimelineError
sealed class TimelineAction

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
    data object Single : TimelineKeyParams
    data class Collection(
        val size: Int,
        val filter: List<Filter<SD>> = emptyList(),
        val sort: Sort? = null
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
    private val posts: List<TimelineData.Post>
) : FeedService {

    override suspend fun get(key: CK): TimelineData.Feed {
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
    private val posts: MutableMap<SK, TimelineData.Post>
) : PostService {
    override suspend fun get(key: SK): TimelineData.Post? {
        return posts[key]
    }

    override suspend fun update(key: SK, value: TimelineData.Post) {
        posts[key] = value
    }

}


class Backend {

    private val posts = mutableMapOf<SK, TimelineData.Post>()

    init {
        (1..200).map { TimelineData.Post(it, "Post $it") }.forEach { this.posts[PagingKey(it.id, TimelineKeyParams.Single)] = it }
    }

    val feedService: FeedService = RealFeedService(posts.values.toList())
    val postService: PostService = RealPostService(posts)
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

            TimelineKeyParams.Single -> {
                val sk = PagingKey(key.key, TimelineKeyParams.Single)
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
            when (key.params) {
                TimelineKeyParams.Single -> {
                    if (value is PagingData.Single) {
                        val updatedValue = value.data
                        if (updatedValue is TimelineData.Post) {
                            val sk = PagingKey(key.key, TimelineKeyParams.Single)
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