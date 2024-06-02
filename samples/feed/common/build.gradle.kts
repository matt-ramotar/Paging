plugins {
    id("storex.android.library")
    kotlin("android")
    alias(libs.plugins.compose)
}

android {
    namespace = "common.feed"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
}

dependencies {
    api(compose.runtime)
    api(compose.material3)
    api(libs.androidx.appCompat)
    api(libs.androidx.compose.activity)
    api(libs.androidx.core)
    api(libs.kotlinx.coroutines.android)
    api(libs.kotlinx.serialization.core)
    api(libs.kotlinx.serialization.json)
    api(libs.circuit.foundation)
    api(libs.coil.compose)
    api(libs.coil.network)
    api(libs.ktor.client.android)
    api(libs.ktor.serialization.json)
    api(libs.ktor.negotiation)
}