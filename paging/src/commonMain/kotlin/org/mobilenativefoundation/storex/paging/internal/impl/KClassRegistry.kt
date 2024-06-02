package org.mobilenativefoundation.storex.paging.internal.impl

import org.mobilenativefoundation.storex.paging.Identifiable
import org.mobilenativefoundation.storex.paging.Quantifiable
import kotlin.reflect.KClass

class KClassRegistry<Id : Comparable<Id>,Q: Quantifiable<Id>, K : Any, V : Identifiable<Id, Q>, E : Any>(
    val id: KClass<Id>,
    val q: KClass<Q>,
    val key: KClass<K>,
    val value: KClass<V>,
    val error: KClass<E>,
)