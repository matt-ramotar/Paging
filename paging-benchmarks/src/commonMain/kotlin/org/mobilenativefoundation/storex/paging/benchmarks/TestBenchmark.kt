package org.mobilenativefoundation.storex.paging.benchmarks

import kotlinx.benchmark.*


@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(BenchmarkTimeUnit.MILLISECONDS)
class TestBenchmark {
    @Benchmark
    fun add() {

    }
}