# Paging

[![codecov](https://codecov.io/gh/matt-ramotar/Paging/graph/badge.svg?token=62YL5HZR9Q)](https://codecov.io/gh/matt-ramotar/Paging)

Powerful and extensible library for efficient paging in Kotlin Multiplatform projects. Built with a focus on performance, flexibility, and ease of use.

## Features

- Seamless integration with `Store` for optimized data management
- Support for local mutations and streaming of child items within the list of paging items
- Highly customizable with support for custom reducers, middleware, and post-reducer effects
- Modular architecture with unidirectional data flow for easy reasoning and maintenance
- Built-in support for logging and error handling
- Extensible design to accommodate various data sources and paging strategies

## Getting Started

### Installation

Add the following dependency to your project:

```kotlin
dependencies {
    implementation("org.mobilenativefoundation.paging:core:1.0.0")
}
```

### Basic Usage

#### 1. Create a `PagingConfig` to configure the paging behavior:

```kotlin
val pagingConfig = PagingConfig(
    pageSize = 20,
    prefetchDistance = 10,
    insertionStrategy = InsertionStrategy.APPEND
)
```

#### 2. Implement a `PagingSource` to provide data for pagination:

```kotlin
val pagingSource = DefaultPagingSource(
    streamProvider = store.pagingSourceStreamProvider(keyFactory)
)
```

#### 3. Configure the `Pager` using `PagerBuilder`:

```kotlin
val pager = PagerBuilder<Int, Int, MyParams, MyData, MyCustomError, MyCustomAction>(
    initialKey = PagingKey(key = 1, params = MyParams()),
    anchorPosition = anchorPositionFlow,
)
    .pagingConfig(pagingConfig)

    .pagerBufferMaxSize(100)
    
    // Provide a custom paging source
    .pagingSource(MyCustomPagingSource())
    
    // Or, use the default paging source
    .defaultPagingSource(MyPagingSourceStreamProvider())
    
    // Or, use Store as your paging source
    .mutableStorePagingSource(mutableStore)

    // Use the default reducer
    .defaultReducer {
        errorHandlingStrategy(ErrorHandlingStrategy.RetryLast(3))
        customActionReducer(MyCustomActionReducer())
    }
    
    // Or, provide a custom reducer
    .reducer(MyCustomReducer())

    // Add custom middleware
    .middleware(MyCustomMiddleware1())
    .middleware(MyCustomMiddleware2())

    // Add custom post-reducer effects
    .effect<SomePagingAction, SomePagingState>(
        action = SomePagingAction::class,
        state = SomePagingState::class,
        effect = MyCustomEffect1()
    )

    .effect<SomePagingAction, SomePagingState>(
        action = SomePagingAction::class,
        state = SomePagingState::class,
        effect = MyCustomEffect2()
    )

    // Use the default logger
    .defaultLogger()

    .build()
```

#### 4. Observe the paging state and dispatch actions:

```kotlin
pager.state.collect { state ->
    when (state) {
        is PagingState.Loading -> {
            // Show loading indicator
            InitialLoadingView()
        }

        is PagingState.Data.Idle -> {
            // Update UI with loaded data
            DataView(pagingItems = state.data) { action ->
                pager.dispatch(action)
            }
        }

        is PagingState.Error -> {
            // Handle error state
            ErrorViewCoordinator(errorState = state) { action ->
                pager.dispatch(action)
            }
        }
    }
}
```

## Documentation

Please check out our website for detailed documentation on architecture, customizations, and advanced usage.

## Contributing

Please read our contributing guidelines to get started.

## License

```
Copyright 2024 Mobile Native Foundation

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
