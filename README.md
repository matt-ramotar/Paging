# 📄 StoreX Paging

[![codecov](https://codecov.io/gh/matt-ramotar/Paging/graph/badge.svg?token=62YL5HZR9Q)](https://codecov.io/gh/matt-ramotar/Paging)

A paging library for large-scale Kotlin applications. Powered by [Store](https://github.com/MobileNativeFoundation/Store), [Molecule](https://github.com/cashapp/molecule), and Compose Runtime.

- **Performance**: This has been our highest priority. Preliminary performance comparisons are showing roughly the same load time and memory utilization as AndroidX Paging, but fewer recompositions
  and janky frame delays.
- **UDF-first design**: This core architecture makes state management clean and predictable. Related AndroidX Pager issue: https://issuetracker.google.com/issues/183495984.
- **“Updating item” concept**: Each item in a list can update independently. Related AndroidX Pager issue: https://issuetracker.google.com/issues/160232968.
- **Operation pipelines**: These operations are applied to ItemSnapshotList instances. This opens up on-the-fly data transformations, such as filtering, grouping, deduplicating, validating, or
  enrichment. Related AndroidX Pager issue: https://issuetracker.google.com/issues/175430431.
- **Local and remote mutations**: StoreX Paging handles both local and remote data changes.
- **Customization options**: Many customization points including launch effects, middleware, side effects, eager loading strategies, fetching strategies, and error handling strategies.
- **AndroidX Paging compatibility**: StoreX Paging is designed to be compatible with AndroidX Paging, facilitating easier migration or integration.
- **Kotlin Multiplatform support**: Targeting all major Kotlin platforms.

## [Circuit](https://github.com/slackhq/circuit) Sample

### Getting Set Up

1. Define your item ID, page request key, and item value types:

```kotlin
value class Cursor(val value: String)
```

```kotlin
data class TimelineRequest(
    val cursor: Cursor?,
    val size: Int,
    val headers: Map<String, String>
)
```

```kotlin
data class Post(
    override val id: Cursor,
    val title: String,
    val body: String,
    val authorId: String,
    val createdAt: LocalDateTime,
    val retweetCount: Int,
    val favoriteCount: Int,
    val commentCount: Int,
    val relevanceScore: Float,
    val trendingScore: Float,
    val isLikedByUser: Boolean
)
```

2. Implement an `IdExtractor`:

```kotlin
val idExtractor = IdExtractor<Cursor, Post> { post -> post.id }
```

3. Implement a `PagingSource`:

```kotlin
class TimelinePagingSource : PagingSource<Cursor, TimelineRequest, Post> {
    override suspend fun load(params: PagingSource.LoadParams<TimelineRequest>):
        PagingSource.LoadResult<Cursor, TimelineRequest, Post> {
        TODO("Fetch posts from network")
    }
}
```

4. Build a `PagingScope`:

```kotlin
PagingScope
    .builder<Cursor, TimelineRequest, Post>(pagingConfig)
    .setIdExtractor(idExtractor)
    .setPagingSource(TimelinePagingSource())
    .addLaunchEffects(launchEffect1, launchEffectN)
    .addSideEffects(sideEffect1, sideEffectN)
    .addMiddleware(middleware1, middlewareN)
    .addFetchingStrategy(customFetchingStrategy)
    .addErrorHandlingStrategy(customErrorHandlingStrategy)
    .build()
```

5. Provide the `PagingScope` to the compose environment at the appropriate level in the composition hierarchy:

```kotlin
class TimelineScreenUi(private val pagingScope: PagingScope) : Ui<TimelineScreen.State> {
    @Composable
    override fun Content(state: TimelineScreen.State, modifier: Modifier) {
        PagingScope(pagingScope) {
            LazyUpdatingItems(state.ids, modifier) { model: UpdatingItem<Cursor, Post> ->
                TimelinePostUi(model)
            }
        }
    }
}
```

6. Provide `PagingState` to the appropriate composable UI:

```kotlin
class TimelineScreenPresenter(
    private val pager: Pager<Cursor>,
    private val dispatcher: Dispatcher<GetFeedRequest>
) : Presenter<TimelineScreen.State> {
    @Composable
    override fun present(): TimelineScreen.State {
        val pagingState = pager.flow.collectAsState()
        return TimelineScreen.State(
            ids = pagingState.ids,
            eventSink = { event ->
                when (event) {
                    TimelineScreen.Event.Refresh -> dispatcher.dispatch(Action.refresh())
                }
            }
        )
    }
}
```

### Adding Sorting and Filtering

1. Implement the `Operation` interface for customized sorting and filtering:

```kotlin
class SortForTimeRange(private val timeRange: TimeRange) :
    Operation<Cursor, TimelineRequest, Post> {
    override fun shouldApply(
        key: TimelineRequest?,
        pagingState: PagingState<Cursor>,
        fetchingState: FetchingState<Cursor, TimelineRequest>
    ): Boolean {
        // Always apply
        return true
    }

    override fun apply(
        snapshot: ItemSnapshotList<Cursor, Post>,
        key: TimelineRequest?,
        pagingState: PagingState<Cursor>,
        fetchingState: FetchingState<Cursor, TimelineRequest>
    ): ItemSnapshotList<Cursor, Post> {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)

        // Separate loaded items and placeholders
        val (loadedItems, placeholders) = snapshot.partition { it != null }

        // Sort and filter loaded items
        val sortedItems = loadedItems.filterNotNull()
            .filter { post -> isWithinRange(post.createdAt, timeRange, now) }
            .sortedWith(
                compareByDescending<Post> { it.favoriteCount }.thenByDescending { it.createdAt }
            )

        // Combine sorted items with placeholders at the end
        val result = sortedItems + placeholders
        return ItemSnapshotList(result)
    }

    private fun isWithinRange(
        createdAt: LocalDateTime,
        timeRange: TimeRange,
        now: LocalDateTime
    ): Boolean {
        val durationSinceCreation = now.toInstant(TimeZone.UTC) - createdAt.toInstant(TimeZone.UTC)
        return durationSinceCreation < timeRange.duration
    }
}
```

2. Update the `OperationPipeline` based on user configuration

```kotlin
class TimelineScreenPresenter(...) : Presenter<TimelineScreen.State> {
    @Composable
    override fun present(): TimelineScreen.State {
        val pagingState = pager.flow.collectAsState()
        var sortingMethod by remember { mutableStateOf<SortingMethod>(SortingMethod.New) }

        LaunchedEffect(sortingMethod) {
            val operation = when (sortingMethod) {
                is Top -> SortForTimeRange(operation.timeRange)
            }
            operationPipeline.clear().add(operation)
        }

        return TimelineScreen.State(
            ids = pagingState.ids,
            eventSink = { event ->
                when (event) {
                    TimelineScreen.Event.Refresh -> dispatcher.dispatch(Action.refresh())
                    TimelineEvent.UpdateSort -> sortingMethod = event.sortingMethod
                }
            }
        )
    }
}
```

### Adding Mutations

```kotlin
@Composable
fun TimelinePostUi(model: UpdatingItem<Cursor, Post>) {
    val coroutineScope = rememberCoroutineScope()
    val state = model.collectAsState(coroutineScope)
    val post = state.value.item

    if (post == null) {
        TimelinePostPlaceholderUi()
    } else {
        TimelinePostLoadedUi(
            post = post,
            updatePost = { updatedPost -> model.emit(UpdatingItem.Event.Update(updatedPost)) }
        )
    }
}

@Composable
fun TimelinePostLoadedUi(
    post: Post,
    updatePost: (next: Post) -> Unit
) {
    val isLikedByUser by remember { derivedStateOf { post.isLikedByUser } }

    Column {
        // ...

        LikeAction(
            isLikedByUser = isLikedByUser,
            onClick = { updatePost(post.copy(isLikedByUser = !isLikedByUser)) }
        )
    }
}
```

### Using a Custom Comparator

```kotlin

object CursorComparator : Comparator<Cursor> {

  override fun compare(a: Cursor, b: Cursor): Int {
    val parsedCursorA = parseCursor(a)
    val parsedCursorB = parseCursor(b)
    return parsedCursorA.first.compareTo(parsedCursorB.first)
  }

  override fun distance(a: Cursor, b: Cursor): Int {
    val parsedCursorA = parseCursor(a)
    val parsedCursorB = parseCursor(b)

    // Compare timestamps
    val timeDiff = parsedCursorA.first - parsedCursorB.first

    return when {
      // If timestamps are different, use their difference
      // Coercing to Int range to avoid overflow issues
      timeDiff != 0L -> timeDiff.coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong()).toInt()

      // If timestamps are the same, compare the unique parts lexicographically
      // This ensures a consistent, deterministic ordering
      else -> parsedCursorA.second.compareTo(parsedCursorB.second)
    }
  }

  private fun parseCursor(cursor: Cursor): Pair<Long, String> {
    // Parsing the cursor string into its components
    val parts = cursor.split('-')
    require(parts.size == 2) { "Invalid cursor format. Expected 'timestamp-uniqueId'" }

    // Converting the timestamp string to a Long for numerical comparison
    val timestamp = parts[0].toLongOrNull() ?: throw IllegalArgumentException("Invalid timestamp in cursor")
    val uniqueId = parts[1]

    return timestamp to uniqueId
  }
}
```

## Contributing

Reach out at https://kotlinlang.slack.com/archives/C06007Z01HU

## License

```txt
Copyright 2024 Mobile Native Foundation

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
