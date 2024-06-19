package org.mobilenativefoundation.storex.paging

import androidx.compose.runtime.*
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.experimental.ExperimentalTypeInference


@Composable
fun <Id : Identifier<Id>, K : Comparable<K>, V : Identifiable<Id>> PagingScope(
    pagingScope: PagingScope<Id, K, V>,
    content: @Composable () -> Unit
) {

    CompositionLocalProvider(
        LocalPagingScope provides pagingScope
    ) {
        content()
    }
}


val LocalPagingScope: ProvidableCompositionLocal<PagingScope<*, *, *>> =
    staticCompositionLocalOf { throw IllegalStateException("PagingScope not provided") }


val LocalSelfUpdatingItemFactory: ProvidableCompositionLocal<SelfUpdatingItemFactory<*, *>> =
    staticCompositionLocalOf { throw IllegalStateException("SelfUpdatingItemFactory not provided") }

val LocalPager: ProvidableCompositionLocal<Pager<*, *, *>> =
    staticCompositionLocalOf { throw IllegalStateException("Pager not provided") }


@Composable
inline fun <Id : Identifier<Id>, V : Identifiable<Id>> SelfUpdatingItem<Id, V>?.stateIn(
    scope: CoroutineScope,
    key: Any? = null
): StateFlow<ItemState<Id, V>> {

    return remember(key) {
        scope.launchMolecule(RecompositionMode.ContextClock) {
            this?.invoke() ?: ItemState.initial()
        }
    }
}

@Composable
inline fun <Id : Identifier<Id>, V : Identifiable<Id>> rememberSelfUpdatingItem(
    id: Id?
): SelfUpdatingItem<Id, V>? {
    if (id == null) {
        return null
    }

    val selfUpdatingItemFactory = LocalSelfUpdatingItemFactory.current as SelfUpdatingItemFactory<Id, V>
    return remember(id) {
        selfUpdatingItemFactory.createSelfUpdatingItem(id)
    }
}


@Composable
inline fun <Id : Identifier<Id>, K : Comparable<K>, V : Identifiable<Id>> Pager<Id, K, V>.collectAsState(
    requests: PagingRequestFlow<K>,
    key: Any = Unit,
    coroutineScope: CoroutineScope = rememberCoroutineScope()
): State<PagingState<Id>> {
    val pagingStateFlow = remember(key) {
        pagingStateFlow(coroutineScope, requests)
    }

    val pagingState = pagingStateFlow.collectAsState()

    return pagingState
}

@Composable
inline fun <Id : Identifier<Id>, K : Comparable<K>, V : Identifiable<Id>> Pager<Id, K, V>.collectAsState(
    key: Any = Unit,
    coroutineScope: CoroutineScope = rememberCoroutineScope()
): State<PagingStateWithEventSink<Id, K>> {

    val requests = remember(key) {
        MutableSharedFlow<PagingRequest<K>>(replay = 20)
    }

    val pagingStateFlow = remember(key) {
        pagingStateFlow(coroutineScope, requests)
    }

    val pagingState = pagingStateFlow.map {
        PagingStateWithEventSink(
            it.ids,
            it.loadStates,
        ) { event ->
            coroutineScope.launch {
                requests.emit(event)
            }
        }
    }.collectAsState(
        PagingStateWithEventSink(
            pagingStateFlow.value.ids,
            pagingStateFlow.value.loadStates
        ) { event ->
            coroutineScope.launch {
                requests.emit(event)
            }
        }
    )

    return pagingState
}


@OptIn(ExperimentalTypeInference::class)
@Composable
inline fun <Id : Identifier<Id>, K : Comparable<K>, V : Identifiable<Id>> Pager<Id, K, V>.collectAsState(
    key: Any = Unit,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    @BuilderInference noinline block: suspend FlowCollector<PagingRequest<K>>.() -> Unit
): State<PagingState<Id>> {
    val pagingStateFlow = remember(key) {
        pagingStateFlow(coroutineScope, flow(block))
    }

    val pagingState = pagingStateFlow.collectAsState()

    return pagingState
}


typealias PagingRequestFlow<K> = Flow<PagingRequest<K>>