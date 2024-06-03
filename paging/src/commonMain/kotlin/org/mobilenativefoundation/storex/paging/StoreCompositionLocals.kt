package org.mobilenativefoundation.storex.paging

import androidx.compose.runtime.*
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.experimental.ExperimentalTypeInference

@Composable
fun <Id : Comparable<Id>, Q : Quantifiable<Id>, K : Any, V : Identifiable<Id, Q>, E : Any> StoreCompositionLocals(
    pager: Pager<Id, Q, K, V, E>,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalSelfUpdatingItemFactory provides pager,
        LocalPager provides pager,
    ) {
        content()
    }
}


val LocalSelfUpdatingItemFactory: ProvidableCompositionLocal<SelfUpdatingItemFactory<*, *, *, *>> =
    staticCompositionLocalOf { throw IllegalStateException("SelfUpdatingItemFactory not provided") }

val LocalPager: ProvidableCompositionLocal<Pager<*, *, *, *, *>> =
    staticCompositionLocalOf { throw IllegalStateException("Pager not provided") }


@Composable
inline fun <Id : Comparable<Id>, Q : Quantifiable<Id>, V : Identifiable<Id, Q>, E : Any> SelfUpdatingItem<Id, Q, V, E>?.stateIn(
    scope: CoroutineScope,
    key: Any? = null
): StateFlow<ItemState<Id, Q, V, E>> {

    return remember(key) {
        scope.launchMolecule(RecompositionMode.ContextClock) {
            this?.invoke() ?: ItemState.initial()
        }
    }
}

@Suppress("UNCHECKED_CAST")
@Composable
inline fun <Id : Comparable<Id>, Q : Quantifiable<Id>, V : Identifiable<Id, Q>, E : Any> rememberSelfUpdatingItem(
    id: Q?
): SelfUpdatingItem<Id, Q, V, E>? {
    if (id == null) {
        return null
    }

    val selfUpdatingItemFactory = LocalSelfUpdatingItemFactory.current as SelfUpdatingItemFactory<Id, Q, V, E>
    return remember(id) {
        selfUpdatingItemFactory.createSelfUpdatingItem(id)
    }
}


@Composable
inline fun <Id : Comparable<Id>, Q : Quantifiable<Id>, K : Any, V : Identifiable<Id, Q>, E : Any> Pager<Id, Q, K, V, E>.collectAsState(
    requests: PagingRequestFlow<K>,
    key: Any = Unit,
    coroutineScope: CoroutineScope = rememberCoroutineScope()
): State<PagingState<Id, Q, E>> {
    val pagingStateFlow = remember(key) {
        pagingStateFlow(coroutineScope, requests)
    }

    val pagingState = pagingStateFlow.collectAsState()

    return pagingState
}

@Composable
inline fun <Id : Comparable<Id>, Q : Quantifiable<Id>, K : Any, V : Identifiable<Id, Q>, E : Any> Pager<Id, Q, K, V, E>.collectAsState(
    key: Any = Unit,
    coroutineScope: CoroutineScope = rememberCoroutineScope()
): State<PagingStateWithEventSink<Id, Q, K, E>> {

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
inline fun <Id : Comparable<Id>, Q : Quantifiable<Id>, K : Any, V : Identifiable<Id, Q>, E : Any> Pager<Id, Q, K, V, E>.collectAsState(
    key: Any = Unit,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    @BuilderInference noinline block: suspend FlowCollector<PagingRequest<K>>.() -> Unit
): State<PagingState<Id, Q, E>> {
    val pagingStateFlow = remember(key) {
        pagingStateFlow(coroutineScope, flow(block))
    }

    val pagingState = pagingStateFlow.collectAsState()

    return pagingState
}


typealias PagingRequestFlow<K> = Flow<PagingRequest<K>>