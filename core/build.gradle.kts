import org.jetbrains.dokka.gradle.DokkaTask

plugins {
    kotlin("multiplatform")
    id("com.android.library")
    id("org.jetbrains.dokka")
    id("org.jetbrains.kotlinx.kover")
}

kotlin {
    androidTarget()
    jvm()
    iosArm64()
    iosX64()
    linuxX64()
    iosSimulatorArm64()
    js {
        browser()
        nodejs()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlin.stdlib)
                implementation(libs.touchlab.kermit)
                implementation(libs.kotlinx.coroutines.core)
                api(libs.store)
                implementation(libs.kotlinx.datetime)
            }
        }

        val androidMain by getting
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