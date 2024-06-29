plugins {
    id("storex.android.application")
    alias(libs.plugins.compose)
    alias(libs.plugins.kotlin.plugin.parcelize)
    alias(libs.plugins.ksp)

}

android {
    namespace = "app.feed.storex"

    defaultConfig {
        applicationId = "storex.feed"
        versionCode = 1
        versionName = "1.0"
    }

    packaging {
        resources {
            excludes.add("/META-INF/{AL2.0,LGPL2.1}")
            excludes.add("/META-INF/versions/9/previous-compilation-data.bin")
        }
    }
}

dependencies {
    implementation(projects.paging)
    implementation(projects.samples.feed.common)
    implementation(libs.kotlinInject.runtime)
    ksp(libs.kotlinInject.compiler)
}
ksp {
    arg("me.tatarka.inject.generateCompanionExtensions", "true")
}