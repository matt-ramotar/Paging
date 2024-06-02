package org.mobilenativefoundation.storex.paging

import androidx.compose.runtime.*
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

@Composable
fun <Id : Comparable<Id>, Q: Quantifiable<Id>, V : Identifiable<Id, Q>, E : Any> StoreCompositionLocals(
    selfUpdatingItemFactory: SelfUpdatingItemFactory<Id, Q, V, E>,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalSelfUpdatingItemFactory provides selfUpdatingItemFactory,
    ) {
        content()
    }
}



val LocalSelfUpdatingItemFactory: ProvidableCompositionLocal<SelfUpdatingItemFactory<*,*, *, *>> =
    staticCompositionLocalOf { throw IllegalStateException("SelfUpdatingItemFactory not provided") }


@Suppress("UNCHECKED_CAST")
@Composable
inline fun <Id : Comparable<Id>, Q: Quantifiable<Id>, V : Identifiable<Id, Q>, E : Any> selfUpdatingItem(id: Q?): SelfUpdatingItem<Id, Q, V, E>? {
    if (id == null) {
        return null
    }

    val selfUpdatingItemFactory = LocalSelfUpdatingItemFactory.current as SelfUpdatingItemFactory<Id, Q, V, E>
    return selfUpdatingItemFactory.createSelfUpdatingItem(id)
}


@Composable
inline fun <Id : Comparable<Id>, Q: Quantifiable<Id>, V : Identifiable<Id, Q>, E : Any> SelfUpdatingItem<Id, Q, V, E>?.stateIn(scope: CoroutineScope): StateFlow<ItemState<Id, Q, V, E>> {

    return scope.launchMolecule(RecompositionMode.ContextClock) {
        this?.invoke() ?: ItemState.initial()
    }
}
