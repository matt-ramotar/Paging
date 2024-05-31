plugins {
    alias(libs.plugins.kotlinx.benchmark)
    alias(libs.plugins.allopen)
    id("storex.multiplatform")
}

allOpen {
    annotation("org.openjdk.jmh.annotations.State")
    annotation("org.openjdk.jmh.annotations.BenchmarkMode")
}

benchmark {
    targets {
        register("jvm")
    }
}

kotlin {
    jvm()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinx.benchmark.runtime)
                implementation(projects.paging)
                implementation(projects.pagingTest)
            }
        }
    }
}