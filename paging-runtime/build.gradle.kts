import dev.mokkery.MockMode

plugins {
    id("storex.android.library")
    id("storex.multiplatform")
    id("storex.compose")
}

kotlin {

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlin.stdlib)
                implementation(libs.touchlab.kermit)
                implementation(libs.kotlinx.coroutines.core)
                api(libs.store)
                api(libs.store.cache)
                api(libs.kotlinx.serialization.core)
                api(libs.kotlinx.serialization.json)
                implementation(libs.jetbrains.compose.runtime)
                implementation(libs.molecule.runtime)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(libs.bundles.testing)
                implementation(libs.kotlinx.datetime)
            }
        }
    }
}

android {
    namespace = "org.mobilenativefoundation.paging.runtime"

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")

    compileSdk = libs.versions.android.compileSdk.get().toInt()
}