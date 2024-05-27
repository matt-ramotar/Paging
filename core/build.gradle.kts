import org.jetbrains.dokka.gradle.DokkaTask

plugins {
    kotlin("multiplatform")
    id("com.android.library")
    id("org.jetbrains.dokka")
    id("org.jetbrains.kotlinx.kover")
    alias(libs.plugins.compose)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.serialization)
}

kotlin {
    androidTarget()
    jvm()
    applyDefaultHierarchyTemplate()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlin.stdlib)
                implementation(libs.touchlab.kermit)
                implementation(libs.kotlinx.coroutines.core)
                api(libs.store)
                api(libs.store.cache)
                // implementation(libs.kotlinx.datetime)


                implementation(libs.kotlin.stdlib)
                implementation(libs.kotlinx.coroutines.core)
                // api(libs.kotlinx.datetime)
                api(libs.kotlinx.serialization.core)
                api(libs.kotlinx.serialization.json)
                api(compose.runtime)
                implementation(libs.molecule.runtime)

                implementation(libs.sqldelight.coroutines)
                implementation(libs.sqldelight.paging)
                implementation(libs.sqldelight.primitive)
            }
        }

        androidMain {
            dependencies {
                api(libs.sqldelight.android)
            }
        }

        jvmMain {
            dependencies {
                api(libs.sqldelight.sqlite)
            }
        }

        nativeMain {
            dependencies {
                api(libs.sqldelight.native)

                implementation("co.touchlab:stately-common:2.0.7")
                implementation("co.touchlab:stately-isolate:2.0.7")
                implementation("co.touchlab:stately-iso-collections:2.0.7")
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.turbine)
                implementation(libs.kotlinx.coroutines.test)
            }
        }
    }

    jvmToolchain(17)
}

android {
    namespace = "org.mobilenativefoundation.paging.core"

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")

    compileSdk = libs.versions.android.compileSdk.get().toInt()
}

tasks.withType<DokkaTask>().configureEach {
    dokkaSourceSets.configureEach {
        reportUndocumented.set(false)
        skipDeprecated.set(true)
        jdkVersion.set(17)
    }
}

koverMerged {
    enable()

    xmlReport {
        onCheck.set(true)
        reportFile.set(layout.projectDirectory.file("kover/coverage.xml"))
    }

    htmlReport {
        onCheck.set(true)
        reportDir.set(layout.projectDirectory.dir("kover/html"))
    }

    verify {
        onCheck.set(true)
    }
}

sqldelight {
    databases {
        create("PagingDb") {
            packageName = "org.mobilenativefoundation.storex.paging"
        }
    }
}
