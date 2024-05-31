
pluginManagement {
    includeBuild("tooling")
}


plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}


enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
rootProject.name = "storex-paging"


include(":paging")
include(":paging-benchmarks")
include(":paging-test")
