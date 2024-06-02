plugins {
    id("storex.android.application")
    alias(libs.plugins.compose)
    alias(libs.plugins.kotlin.plugin.parcelize)
}

android {
    namespace = "app.feed.androidx"

    defaultConfig {
        applicationId = "app.feed.androidx"
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
    implementation(libs.androidx.paging)
    implementation(projects.samples.feed.common)
}