package org.mobilenativefoundation.storex.paging.internal.impl

import org.mobilenativefoundation.store5.core.Identifiable
import kotlin.reflect.KClass

class KClassRegistry<Id : Comparable<Id>, K : Any, V : Identifiable<Id>, E : Any>(
    val id: KClass<Id>,
    val key: KClass<K>,
    val value: KClass<V>,
    val error: KClass<E>
)