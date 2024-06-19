package org.mobilenativefoundation.storex.paging

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow
import org.mobilenativefoundation.storex.paging.custom.Operation
import kotlin.reflect.KClass

interface PagingScope<Id : Identifier<*>, K : Comparable<K>, V : Identifiable<Id>> {


    val pager: PagerV2<Id>
    val operator: Operator<Id, K, V>
    val updatingItemProvider: UpdatingItemProvider<Id, V>
    val dispatcher: Dispatcher<K>
}

interface PagerV2<Id : Identifier<*>> {
    val flow: Flow<PagingState<Id>>

    class Builder<Id : Identifier<*>, K : Comparable<K>, V : Identifiable<Id>>(
        private val idKClass: KClass<Id>,
        private val keyKClass: KClass<K>,
        private val valueKClass: KClass<V>
    ) {
        fun build(): PagerV2<Id> {
            TODO()
        }
    }
}

interface UpdatingItemV2<Id : Identifier<*>, V : Identifiable<Id>> {
    @Composable
    operator fun invoke(): ItemState<Id, V>

    class State<Id : Identifier<*>, V : Identifiable<Id>>(
        val value: V?,
        val loadState: SingleLoadState,
    ) {
        internal var version: Long = 0
    }

    sealed interface Event<Id : Identifier<*>, V : Identifiable<Id>> {
        data object Refresh : Event<Nothing, Nothing>
        data object Clear : Event<Nothing, Nothing>
        data class Update<Id : Identifier<*>, V : Identifiable<Id>>(val value: V) : Event<Id, V>
    }
}

interface UpdatingItemProvider<Id : Identifier<*>, V : Identifiable<Id>> {
    operator fun get(id: Id): UpdatingItemV2<Id, V>
}

interface Operator<Id : Identifier<*>, K : Any, V : Identifiable<Id>> {
    fun add(operation: Operation<Id, K, V>)
    fun remove(operation: Operation<Id, K, V>)
    fun removeAll(predicate: (Operation<Id, K, V>) -> Boolean)
    fun clear()
    fun get(): List<Operation<Id, K, V>>
    fun get(predicate: (Operation<Id, K, V>) -> Boolean)
}

interface Dispatcher<K : Comparable<K>> {
    fun dispatch(key: K)
}

