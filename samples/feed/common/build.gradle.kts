plugins {
    id("storex.android.library")
    kotlin("android")
    alias(libs.plugins.compose)
    alias(libs.plugins.kotlin.plugin.parcelize)
    alias(libs.plugins.serialization)
}

android {
    namespace = "app.feed.common"
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

    implementation(projects.paging)

    api("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.1")

    // ViewModel utilities for Compose
    api("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.1")
}