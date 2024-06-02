package org.mobilenativefoundation.storex.paging


/*
 * Copyright (C) 2022 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import androidx.compose.runtime.MonotonicFrameClock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlin.coroutines.CoroutineContext
import kotlin.experimental.ExperimentalTypeInference

/** The different recomposition modes of Molecule. */
public enum class RecompositionMode {
    /**
     * When a recomposition is needed, use a [MonotonicFrameClock] pulled from the calling [CoroutineContext]
     * to determine when to run. If no clock is found in the context, an exception is thrown.
     *
     * Use this option to drive Molecule with a built-in frame clock or a custom one.
     */
    ContextClock,

    /**
     * Run recomposition eagerly whenever one is needed.
     * Molecule will emit a new item every time the snapshot state is invalidated.
     */
    Immediate,
}

fun RecompositionMode.toCashRecompositionMode() = when (this) {
    RecompositionMode.ContextClock -> app.cash.molecule.RecompositionMode.ContextClock
    RecompositionMode.Immediate -> app.cash.molecule.RecompositionMode.Immediate
}

//
//fun <Id : Comparable<Id>, K : Any, V : Identifiable<Id>, E : Any> CoroutineScope.launchPager(
//    pager: Pager<Id, K, V, E>,
//    requests: Flow<PagingRequest<K>>,
//    recompositionMode: RecompositionMode = RecompositionMode.ContextClock
//): StateFlow<PagingState<Id, E>> =
//    this.launchMolecule(recompositionMode.toCashRecompositionMode()) {
//        pager.pagingState(requests)
//    }
//
//fun <Id : Comparable<Id>, K : Any, V : Identifiable<Id>, E : Any> CoroutineScope.launchPager(
//    pager: Pager<Id, K, V, E>,
//    requests: Flow<PagingRequest<K>>,
//): StateFlow<PagingState<Id, E>> =
//    this.launchMolecule(RecompositionMode.Immediate.toCashRecompositionMode()) {
//        pager.pagingState(requests)
//    }
//
//fun <Id : Comparable<Id>, K : Any, V : Identifiable<Id>, E : Any> Pager<Id, K, V, E>.pagingFlow(
//    requests: Flow<PagingRequest<K>>,
//    recompositionMode: RecompositionMode = RecompositionMode.ContextClock
//): Flow<PagingState<Id, E>> =
//    moleculeFlow(recompositionMode.toCashRecompositionMode()) {
//        pagingState(requests)
//    }
//
//
fun <Id : Comparable<Id>, Q : Quantifiable<Id>, K : Any, V : Identifiable<Id, Q>, E : Any> Pager<Id, Q, K, V, E>.launchPagingFlow(
    requests: Flow<PagingRequest<K>> = emptyFlow(),
): Flow<PagingState<Id, Q, E>> = this.pagingFlow(requests, RecompositionMode.Immediate)


@OptIn(ExperimentalTypeInference::class)
fun <Id : Comparable<Id>, Q : Quantifiable<Id>, K : Any, V : Identifiable<Id, Q>, E : Any> Pager<Id, Q, K, V, E>.launchPagingFlow(
    @BuilderInference block: suspend FlowCollector<PagingRequest<K>>.() -> Unit
): Flow<PagingState<Id, Q, E>> = this.pagingFlow(flow(block), RecompositionMode.Immediate)







