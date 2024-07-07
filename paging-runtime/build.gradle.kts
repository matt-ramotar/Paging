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

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlin.test.common)
                implementation(libs.kotlin.test.annotations.common)
                implementation(libs.turbine)
                implementation(libs.kotlinx.coroutines.test)
            }
        }
    }
}

android {
    namespace = "org.mobilenativefoundation.paging.core"

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")

    compileSdk = libs.versions.android.compileSdk.get().toInt()
}
