package org.mobilenativefoundation.storex.paging

import androidx.compose.runtime.*
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

@Composable
fun <Id : Comparable<Id>, Q: Quantifiable<Id>, K: Any, V : Identifiable<Id, Q>, E : Any> StoreCompositionLocals(
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



val LocalSelfUpdatingItemFactory: ProvidableCompositionLocal<SelfUpdatingItemFactory<*,*, *, *>> =
    staticCompositionLocalOf { throw IllegalStateException("SelfUpdatingItemFactory not provided") }

val LocalPager: ProvidableCompositionLocal<Pager<*, *, *, *, *>> =
    staticCompositionLocalOf { throw IllegalStateException("Pager not provided") }



@Composable
inline fun <Id : Comparable<Id>, Q: Quantifiable<Id>, V : Identifiable<Id, Q>, E : Any> SelfUpdatingItem<Id, Q, V, E>?.stateIn(scope: CoroutineScope, key: Any? = null): StateFlow<ItemState<Id, Q, V, E>> {

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
