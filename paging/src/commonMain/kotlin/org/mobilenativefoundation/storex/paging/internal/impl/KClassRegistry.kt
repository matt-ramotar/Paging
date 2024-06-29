package org.mobilenativefoundation.storex.paging.internal.impl

import org.mobilenativefoundation.storex.paging.Identifiable
import org.mobilenativefoundation.storex.paging.Identifier
import kotlin.reflect.KClass

class KClassRegistry<Id : Identifier<*>, K : Any, V : Identifiable<Id>>(
    val id: KClass<Id>,
    val key: KClass<K>,
    val value: KClass<V>,
)