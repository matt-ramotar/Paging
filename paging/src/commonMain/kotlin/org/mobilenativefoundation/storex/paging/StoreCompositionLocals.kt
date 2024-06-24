package org.mobilenativefoundation.storex.paging

import androidx.compose.runtime.*
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.mobilenativefoundation.storex.paging.scope.PagingScope
import org.mobilenativefoundation.storex.paging.scope.UpdatingItemV2


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


@Composable
inline fun <Id : Identifier<Id>, V : Identifiable<Id>> UpdatingItemV2<Id, V>?.stateIn(
    scope: CoroutineScope,
    key: Any? = null
): StateFlow<ItemState<Id, V>> {

    return remember(key) {
        scope.launchMolecule(RecompositionMode.ContextClock) {
            this?.invoke() ?: ItemState.initial()
        }
    }
}

@Suppress("UNCHECKED_CAST")
@Composable
inline fun <Id : Identifier<Id>, V : Identifiable<Id>> rememberSelfUpdatingItem(
    id: Id?
): UpdatingItemV2<Id, V>? {
    if (id == null) {
        return null
    }

    val scope = LocalPagingScope.current as PagingScope<Id, *, V>
    val updatingItemProvider = scope.updatingItemProvider

    return remember(id) { updatingItemProvider[id] }
}


@Composable
inline fun <Id : Identifier<Id>, K : Comparable<K>, V : Identifiable<Id>> PagingScope<Id, K, V>.collectPagingState(
    key: Any = Unit,
    coroutineScope: CoroutineScope = rememberCoroutineScope()
): State<PagingState<Id>> {


    // TODO(): Better way to do this for initial value
    val stateFlow = remember(key) {
        pager.flow.stateIn(coroutineScope, started = SharingStarted.Lazily, initialValue = PagingState.initial())
    }

    return stateFlow.collectAsState()
}

@Composable
inline fun <Id : Identifier<Id>, K : Comparable<K>, V : Identifiable<Id>> PagingScope<Id, K, V>.collectAsState(
    key: Any = Unit,
    coroutineScope: CoroutineScope = rememberCoroutineScope()
): State<PagingStateWithEventSink<Id, K>> {

    val stateFlow = remember(key) {
        pager.flow.stateIn(
            coroutineScope,
            started = SharingStarted.Lazily,
            initialValue = PagingState.initial()
        )
    }

    val pagingState = stateFlow.map {
        PagingStateWithEventSink(
            it.ids,
            it.loadStates,
        ) { event ->
            coroutineScope.launch {
                dispatcher.dispatch(event)
            }
        }
    }.collectAsState(
        PagingStateWithEventSink(
            stateFlow.value.ids,
            stateFlow.value.loadStates
        ) { event ->
            coroutineScope.launch {
                dispatcher.dispatch(event)
            }
        }
    )

    return pagingState
}

typealias PagingRequestFlow<K> = Flow<PagingRequest<K>>