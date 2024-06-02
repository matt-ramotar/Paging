package org.mobilenativefoundation.storex.paging

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

@Composable
fun <Id : Comparable<Id>, Q: Quantifiable<Id>, V : Identifiable<Id, Q>, E : Any> SelfUpdatingItems(
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
inline fun <Id : Comparable<Id>, Q: Quantifiable<Id>, V : Identifiable<Id, Q>, E : Any> selfUpdatingItem(id: Q): SelfUpdatingItem<Id, Q, V, E> {
    val selfUpdatingItemFactory = LocalSelfUpdatingItemFactory.current as SelfUpdatingItemFactory<Id, Q, V, E>
    return selfUpdatingItemFactory.createSelfUpdatingItem(id)
}


